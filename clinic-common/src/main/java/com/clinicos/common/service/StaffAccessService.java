package com.clinicos.common.service;

import com.clinicos.common.dto.ClinicMemberDto;
import com.clinicos.common.dto.StaffAccessDto;
import com.clinicos.common.dto.StaffInviteRequest;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.repository.ClinicUserRepository;
import com.clinicos.common.repository.PasswordResetTokenRepository;
import com.clinicos.common.security.RolePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sign-in access for clinic staff. The owner invites a staff member by email; they choose a password
 * from the link (or sign in with Google using that email) and then see only their role's pages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffAccessService {

    static final int INVITE_VALID_DAYS = 7;
    static final int MAX_INVITES_PER_HOUR = 5;
    static final int MAX_STAFF_ACCOUNTS = 200;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final ObjectProvider<ClinicUserRepository> users;
    private final ObjectProvider<PasswordResetTokenRepository> resetTokens;
    private final ObjectProvider<StaffInviteMailer> mailer;
    private final PasswordAuthService passwords;

    /** Who is calling and what they may use. */
    public ClinicMemberDto me(ClinicUser caller) {
        boolean restricted = caller.isInvitedStaff();
        return ClinicMemberDto.builder()
                .email(caller.getEmail())
                .fullName(caller.getFullName())
                .role(caller.getRole())
                .roleLabel(restricted ? RolePolicy.label(caller.getRole()) : "Owner")
                .owner(!restricted)
                .pages(RolePolicy.pages(caller.getRole(), restricted).stream().map(Enum::name).sorted().toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<StaffAccessDto> list(String clinicId) {
        return users.getObject().findByClinicId(clinicId).stream()
                .filter(ClinicUser::isInvitedStaff)
                .sorted(Comparator.comparing(ClinicUser::getInvitedAt).reversed())
                .map(u -> toDto(u, null, null))
                .toList();
    }

    @Transactional
    public StaffAccessDto invite(ClinicUser actor, StaffInviteRequest input, String clinicName) {
        String clinicId = actor.getClinicId();
        if (input == null) throw bad("Staff details are required.");
        String email = normalize(input.getEmail());
        if (email == null || !EMAIL.matcher(email).matches()) throw bad("Enter the staff member's email address.");
        String role = RolePolicy.normalize(input.getRole());
        if (!RolePolicy.STAFF_ROLES.contains(role)) throw bad("Choose a role for this staff member.");
        if ("ADMIN".equals(role) && actor.isInvitedStaff() && !"ADMIN".equalsIgnoreCase(actor.getRole())) {
            throw new RegistrationException(HttpStatus.FORBIDDEN, "Only the clinic owner or an admin can give admin access.");
        }
        String name = input.getFullName() == null || input.getFullName().isBlank() ? email : input.getFullName().trim();
        if (name.length() > 255) throw bad("Name is too long.");

        ClinicUserRepository repo = users.getObject();
        ClinicUser user = repo.findByEmailIgnoreCase(email).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        boolean sendLink;
        if (user == null) {
            long existing = repo.findByClinicId(clinicId).stream().filter(ClinicUser::isInvitedStaff).count();
            if (existing >= MAX_STAFF_ACCOUNTS) throw conflict("This clinic has reached " + MAX_STAFF_ACCOUNTS + " staff accounts.");
            user = ClinicUser.builder()
                    .email(email)
                    .clinicId(clinicId)
                    .clinicName(clinicName)
                    .status(ClinicUser.UserStatus.APPROVED)
                    .isActive(true)
                    .isSuperAdmin(false)
                    .approvedAt(now)
                    .approvedBy(actor.getEmail())
                    .invitedAt(now)
                    .createdAt(now)
                    .build();
            sendLink = true;
        } else if (!user.isInvitedStaff() || !clinicId.equals(user.getClinicId())) {
            throw conflict(user.isInvitedStaff() || user.getClinicId() == null || !clinicId.equals(user.getClinicId())
                    ? "This email already has a CliniSys One account with another clinic. Use a different email for this staff member."
                    : "This is the clinic owner's account. The owner already has full access.");
        } else {
            boolean wasRevoked = !Boolean.TRUE.equals(user.getIsActive()) || user.getStatus() != ClinicUser.UserStatus.APPROVED;
            user.setStatus(ClinicUser.UserStatus.APPROVED);
            user.setIsActive(true);
            sendLink = wasRevoked || user.getPasswordHash() == null && user.getLastLogin() == null;
        }
        if (Boolean.TRUE.equals(user.getIsSuperAdmin())) throw conflict("This account can't be managed from a clinic.");
        user.setFullName(name);
        user.setRole(role);
        if (input.getPhone() != null && !input.getPhone().isBlank()) user.setPhone(input.getPhone().trim());
        if (input.getStaffMemberId() != null) user.setStaffMemberId(input.getStaffMemberId());
        user.setInvitedBy(actor.getEmail());
        user.setUpdatedAt(now);
        user = repo.save(user);
        log.info("Staff access {} for user id {} in clinic {} by {}", sendLink ? "invited" : "updated", user.getId(), clinicId, actor.getEmail());
        return sendLink ? deliver(user, actor, clinicName) : toDto(user, null, null);
    }

    @Transactional
    public StaffAccessDto resend(ClinicUser actor, String email, String clinicName) {
        ClinicUser user = staff(actor.getClinicId(), email);
        if (!Boolean.TRUE.equals(user.getIsActive())) throw conflict("Access was removed. Invite this person again to restore it.");
        if (resetTokens.getObject().countByUserIdAndCreatedAtAfter(user.getId(), LocalDateTime.now().minusHours(1)) >= MAX_INVITES_PER_HOUR) {
            throw new RegistrationException(HttpStatus.TOO_MANY_REQUESTS, "Several invites were sent in the last hour. Please wait before sending another.");
        }
        return deliver(user, actor, clinicName);
    }

    /** Stops a staff member signing in. Their records and history stay. */
    @Transactional
    public StaffAccessDto revoke(ClinicUser actor, String email) {
        ClinicUser user = staff(actor.getClinicId(), email);
        if (user.getEmail().equalsIgnoreCase(actor.getEmail())) throw conflict("You can't remove your own access.");
        user.setIsActive(false);
        user.setStatus(ClinicUser.UserStatus.SUSPENDED);
        user.setUpdatedAt(LocalDateTime.now());
        resetTokens.getObject().retireOutstanding(user.getId(), LocalDateTime.now());
        log.info("Staff access removed for user id {} in clinic {} by {}", user.getId(), actor.getClinicId(), actor.getEmail());
        return toDto(users.getObject().save(user), null, null);
    }

    private StaffAccessDto deliver(ClinicUser user, ClinicUser actor, String clinicName) {
        String link = passwords.issueInviteLink(user, INVITE_VALID_DAYS);
        StaffInviteMailer delivery = mailer.getIfAvailable();
        boolean sent = false;
        if (delivery != null) {
            try {
                delivery.sendInvite(user.getEmail(), user.getFullName(), clinicName, RolePolicy.label(user.getRole()),
                        actor.getFullName() == null ? actor.getEmail() : actor.getFullName(), link, INVITE_VALID_DAYS);
                sent = true;
            } catch (RuntimeException e) {
                log.error("Staff invite email could not be sent to user id {}: {}", user.getId(), e.getClass().getSimpleName());
            }
        }
        // Without email, the owner gets the link to share; with email it is never returned.
        return toDto(user, sent, sent ? null : link);
    }

    private ClinicUser staff(String clinicId, String email) {
        String key = normalize(email);
        return users.getObject().findByEmailIgnoreCase(key == null ? "" : key)
                .filter(u -> u.isInvitedStaff() && clinicId.equals(u.getClinicId()))
                .orElseThrow(() -> new RegistrationException(HttpStatus.NOT_FOUND, "No staff account with that email in this clinic."));
    }

    static StaffAccessDto toDto(ClinicUser u, Boolean emailSent, String link) {
        String status = !Boolean.TRUE.equals(u.getIsActive()) || u.getStatus() != ClinicUser.UserStatus.APPROVED ? "REVOKED"
                : u.getPasswordHash() == null && u.getLastLogin() == null ? "INVITED" : "ACTIVE";
        return StaffAccessDto.builder()
                .email(u.getEmail()).fullName(u.getFullName()).role(u.getRole()).roleLabel(RolePolicy.label(u.getRole()))
                .staffMemberId(u.getStaffMemberId()).status(status).invitedAt(u.getInvitedAt()).lastLogin(u.getLastLogin())
                .emailSent(emailSent).inviteLink(link)
                .build();
    }

    private static String normalize(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static RegistrationException bad(String message) {
        return new RegistrationException(HttpStatus.BAD_REQUEST, message);
    }

    private static RegistrationException conflict(String message) {
        return new RegistrationException(HttpStatus.CONFLICT, message);
    }
}
