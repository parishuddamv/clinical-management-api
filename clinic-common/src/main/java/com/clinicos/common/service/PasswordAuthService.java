package com.clinicos.common.service;

import com.clinicos.common.dto.ForgotPasswordRequest;
import com.clinicos.common.dto.GoogleAuthResponse;
import com.clinicos.common.dto.PasswordLoginRequest;
import com.clinicos.common.dto.ResetPasswordRequest;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.entity.PasswordResetToken;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.repository.ClinicUserRepository;
import com.clinicos.common.repository.PasswordResetTokenRepository;
import com.clinicos.common.security.JwtTokenProvider;
import com.clinicos.common.security.PasswordHashing;
import com.clinicos.common.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Email/password sign-in and self-service password reset.
 *
 * Sign-in answers with the same shape as Google sign-in, so the web app's approval flow works the
 * same way for both. Failed attempts are counted per account; five in a row lock it for 15 minutes.
 * Forgot-password always answers the same way, so it cannot be used to find out which emails exist.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordAuthService {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final int LOCK_MINUTES = 15;
    static final int RESET_VALID_MINUTES = 30;
    static final int MAX_RESETS_PER_HOUR = 3;

    private static final String INVALID_LOGIN = "We could not sign you in. Check your email and password, then try again.";
    private static final String INVALID_RESET = "This reset link has expired or was already used. Request a new one.";
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Compared against when the account does not exist, so both cases take about the same time. */
    private static final String DUMMY_HASH = PasswordHashing.encoder().encode("no-such-account-0000");

    private final ObjectProvider<ClinicUserRepository> users;
    private final ObjectProvider<PasswordResetTokenRepository> resetTokens;
    private final ObjectProvider<PasswordResetMailer> mailer;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.frontend-url:https://www.clinisysone.com}")
    private String frontendUrl;

    private final PasswordEncoder encoder = PasswordHashing.encoder();

    @Transactional(noRollbackFor = RegistrationException.class)
    public GoogleAuthResponse login(PasswordLoginRequest request) {
        String email = normalize(request == null ? null : request.getEmail());
        String password = request == null ? null : request.getPassword();
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            throw new RegistrationException(HttpStatus.BAD_REQUEST, "Email and password are required.");
        }

        ClinicUserRepository repo = users.getObject();
        Optional<ClinicUser> found = repo.findByEmailIgnoreCase(email);
        if (found.isEmpty() || found.get().getPasswordHash() == null) {
            encoder.matches(password, DUMMY_HASH);
            throw new RegistrationException(HttpStatus.UNAUTHORIZED, INVALID_LOGIN);
        }

        ClinicUser user = found.get();
        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new RegistrationException(HttpStatus.LOCKED,
                    "Too many unsuccessful attempts. Wait " + LOCK_MINUTES + " minutes, or reset your password.");
        }

        if (!encoder.matches(password, user.getPasswordHash())) {
            int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
                user.setFailedLoginAttempts(0);
                log.warn("Email sign-in locked for {} minutes after {} failed attempts (user id {})",
                        LOCK_MINUTES, MAX_FAILED_ATTEMPTS, user.getId());
            } else {
                user.setFailedLoginAttempts(attempts);
            }
            repo.save(user);
            throw new RegistrationException(HttpStatus.UNAUTHORIZED, INVALID_LOGIN);
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(now);
        repo.save(user);
        return session(user);
    }

    @Transactional
    public void requestReset(ForgotPasswordRequest request, String requesterIp) {
        String email = normalize(request == null ? null : request.getEmail());
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new RegistrationException(HttpStatus.BAD_REQUEST, "Enter a valid email address.");
        }

        Optional<ClinicUser> found = users.getObject().findByEmailIgnoreCase(email);
        if (found.isEmpty() || found.get().getStatus() == ClinicUser.UserStatus.SUSPENDED) {
            return; // Same answer as for a real account
        }
        ClinicUser user = found.get();
        PasswordResetTokenRepository tokens = resetTokens.getObject();
        LocalDateTime now = LocalDateTime.now();
        if (tokens.countByUserIdAndCreatedAtAfter(user.getId(), now.minusHours(1)) >= MAX_RESETS_PER_HOUR) {
            log.warn("Password reset rate limit reached for user id {}", user.getId());
            return;
        }

        PasswordResetMailer delivery = mailer.getIfAvailable();
        if (delivery == null) {
            log.error("Password reset requested for user id {} but email delivery is not configured (MAIL_HOST)", user.getId());
            return;
        }

        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        tokens.retireOutstanding(user.getId(), now);
        tokens.save(PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(sha256(token))
                .createdAt(now)
                .expiresAt(now.plusMinutes(RESET_VALID_MINUTES))
                .requestedIp(requesterIp == null ? null : requesterIp.substring(0, Math.min(64, requesterIp.length())))
                .build());

        String url = frontendUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
        try {
            delivery.sendResetLink(user.getEmail(), user.getFullName(), url, RESET_VALID_MINUTES);
            log.info("Password reset link sent to user id {}", user.getId());
        } catch (RuntimeException e) {
            log.error("Password reset email could not be sent to user id {}: {}", user.getId(), e.getClass().getSimpleName());
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new RegistrationException(HttpStatus.BAD_REQUEST, INVALID_RESET);
        }
        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new RegistrationException(HttpStatus.BAD_REQUEST, "Enter the same password in both fields.");
        }

        PasswordResetTokenRepository tokens = resetTokens.getObject();
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken reset = tokens.findByTokenHash(sha256(request.getToken().trim()))
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new RegistrationException(HttpStatus.BAD_REQUEST, INVALID_RESET));

        ClinicUserRepository repo = users.getObject();
        ClinicUser user = repo.findById(reset.getUserId())
                .orElseThrow(() -> new RegistrationException(HttpStatus.BAD_REQUEST, INVALID_RESET));
        String problem = PasswordPolicy.problem(request.getPassword(), user.getEmail()).orElse(null);
        if (problem != null) {
            throw new RegistrationException(HttpStatus.BAD_REQUEST, problem);
        }

        user.setPasswordHash(encoder.encode(request.getPassword()));
        user.setPasswordUpdatedAt(now);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        repo.save(user);
        reset.setUsedAt(now);
        tokens.save(reset);
        tokens.retireOutstanding(user.getId(), now);
        log.info("Password reset completed for user id {}", user.getId());
    }

    private GoogleAuthResponse session(ClinicUser user) {
        boolean approved = user.getStatus() == ClinicUser.UserStatus.APPROVED && Boolean.TRUE.equals(user.getIsActive());
        String clinicId = approved ? user.getClinicId() : null; // Unapproved users get an identity-only token
        String role = Boolean.TRUE.equals(user.getIsSuperAdmin()) ? "SUPER_ADMIN" : user.getRole();
        String message = switch (user.getStatus()) {
            case APPROVED -> approved ? "User approved - welcome back!" : "Account inactive";
            case REJECTED -> "Registration rejected: " + user.getRejectionReason();
            case SUSPENDED -> "Account suspended";
            default -> "Registration pending approval";
        };
        return GoogleAuthResponse.builder()
                .token(jwtTokenProvider.generateToken(user.getEmail(), clinicId))
                .user(GoogleAuthResponse.AuthUser.builder()
                        .id(String.valueOf(user.getId()))
                        .email(user.getEmail())
                        .name(user.getFullName())
                        .clinicId(clinicId)
                        .role(role)
                        .build())
                .clinicId(clinicId)
                .expiresIn(86400)
                .userStatus(user.getStatus().name())
                .isApproved(approved)
                .needsDemoBooking(!approved && user.getStatus() != ClinicUser.UserStatus.REJECTED)
                .message(message)
                .build();
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
