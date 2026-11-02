package com.clinicos.common.service;

import com.clinicos.common.dto.StaffAccessDto;
import com.clinicos.common.dto.StaffInviteRequest;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.repository.ClinicUserRepository;
import com.clinicos.common.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StaffAccessServiceTest {

    private final ClinicUserRepository repo = mock(ClinicUserRepository.class);
    private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    private final StaffInviteMailer mailer = mock(StaffInviteMailer.class);
    private final PasswordAuthService passwords = mock(PasswordAuthService.class);
    private StaffAccessService service;
    private final ClinicUser owner = ClinicUser.builder().id(1L).email("owner@zest.in").fullName("Prasanna Kumar").clinicId("C1").role("DOCTOR")
            .status(ClinicUser.UserStatus.APPROVED).isActive(true).build();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        ObjectProvider<ClinicUserRepository> r = mock(ObjectProvider.class);
        ObjectProvider<PasswordResetTokenRepository> t = mock(ObjectProvider.class);
        ObjectProvider<StaffInviteMailer> m = mock(ObjectProvider.class);
        when(r.getObject()).thenReturn(repo);
        when(t.getObject()).thenReturn(tokens);
        when(m.getIfAvailable()).thenReturn(mailer);
        service = new StaffAccessService(r, t, m, passwords);
        when(repo.save(any())).thenAnswer(inv -> { ClinicUser u = inv.getArgument(0); if (u.getId() == null) u.setId(50L); return u; });
        when(repo.findByClinicId("C1")).thenReturn(List.of(owner));
        when(passwords.issueInviteLink(any(), anyInt())).thenReturn("https://www.clinisysone.com/reset-password?token=abc&invite=1");
    }

    private StaffInviteRequest req(String email, String role) {
        return StaffInviteRequest.builder().email(email).fullName("Justin V").role(role).staffMemberId(7L).build();
    }

    @Test
    void inviteCreatesAnApprovedRestrictedAccountInTheOwnersClinicAndEmailsTheLink() {
        when(repo.findByEmailIgnoreCase("justin@zest.in")).thenReturn(Optional.empty());

        StaffAccessDto dto = service.invite(owner, req(" Justin@Zest.in ", "doctor"), "Zest Clinic");

        ArgumentCaptor<ClinicUser> saved = ArgumentCaptor.forClass(ClinicUser.class);
        verify(repo).save(saved.capture());
        ClinicUser u = saved.getValue();
        assertThat(u.getEmail()).isEqualTo("justin@zest.in");
        assertThat(u.getClinicId()).isEqualTo("C1");
        assertThat(u.getRole()).isEqualTo("DOCTOR");
        assertThat(u.isInvitedStaff()).isTrue();
        assertThat(u.getStatus()).isEqualTo(ClinicUser.UserStatus.APPROVED);
        assertThat(u.getStaffMemberId()).isEqualTo(7L);
        verify(mailer).sendInvite(eq("justin@zest.in"), eq("Justin V"), eq("Zest Clinic"), eq("Doctor"), eq("Prasanna Kumar"), contains("invite=1"), eq(7));
        assertThat(dto.getStatus()).isEqualTo("INVITED");
        assertThat(dto.getEmailSent()).isTrue();
        assertThat(dto.getInviteLink()).isNull();
    }

    @Test
    void withoutEmailDeliveryTheOwnerGetsTheLinkToShare() {
        when(repo.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("smtp down")).when(mailer).sendInvite(any(), any(), any(), any(), any(), any(), anyInt());

        StaffAccessDto dto = service.invite(owner, req("nurse@zest.in", "NURSE"), "Zest Clinic");

        assertThat(dto.getEmailSent()).isFalse();
        assertThat(dto.getInviteLink()).contains("token=abc");
    }

    @Test
    void emailsBelongingToAnotherClinicOrTheOwnerAreRefused() {
        when(repo.findByEmailIgnoreCase("other@x.in")).thenReturn(Optional.of(ClinicUser.builder().email("other@x.in").clinicId("C2").build()));
        when(repo.findByEmailIgnoreCase("owner@zest.in")).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.invite(owner, req("other@x.in", "NURSE"), "Zest"))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("another clinic");
        assertThatThrownBy(() -> service.invite(owner, req("owner@zest.in", "NURSE"), "Zest"))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("clinic owner's account");
        assertThatThrownBy(() -> service.invite(owner, req("a@b.in", "SUPER_ADMIN"), "Zest"))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("role");
    }

    @Test
    void changingAnActiveStaffRoleDoesNotResendTheInvite() {
        ClinicUser existing = ClinicUser.builder().id(9L).email("justin@zest.in").clinicId("C1").role("NURSE").invitedAt(LocalDateTime.now().minusDays(3))
                .status(ClinicUser.UserStatus.APPROVED).isActive(true).passwordHash("x").build();
        when(repo.findByEmailIgnoreCase("justin@zest.in")).thenReturn(Optional.of(existing));

        StaffAccessDto dto = service.invite(owner, req("justin@zest.in", "DOCTOR"), "Zest");

        assertThat(dto.getRole()).isEqualTo("DOCTOR");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
        verifyNoInteractions(mailer);
    }

    @Test
    void staffManagersCannotHandOutAdmin() {
        ClinicUser manager = ClinicUser.builder().id(3L).email("m@zest.in").clinicId("C1").role("MANAGER").invitedAt(LocalDateTime.now()).build();
        assertThatThrownBy(() -> service.invite(manager, req("x@zest.in", "ADMIN"), "Zest"))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("owner or an admin");
    }

    @Test
    void revokeStopsSignInButNeverTouchesTheOwnerOrSelf() {
        ClinicUser staff = ClinicUser.builder().id(9L).email("justin@zest.in").clinicId("C1").role("DOCTOR").invitedAt(LocalDateTime.now())
                .status(ClinicUser.UserStatus.APPROVED).isActive(true).build();
        when(repo.findByEmailIgnoreCase("justin@zest.in")).thenReturn(Optional.of(staff));
        when(repo.findByEmailIgnoreCase("owner@zest.in")).thenReturn(Optional.of(owner));

        assertThat(service.revoke(owner, "justin@zest.in").getStatus()).isEqualTo("REVOKED");
        assertThat(staff.getIsActive()).isFalse();
        verify(tokens).retireOutstanding(eq(9L), any());
        assertThatThrownBy(() -> service.revoke(owner, "owner@zest.in")).isInstanceOf(RegistrationException.class).hasMessageContaining("No staff account");
    }

    @Test
    void meTellsOwnersAndStaffApart() {
        assertThat(service.me(owner).isOwner()).isTrue();
        assertThat(service.me(owner).getPages()).contains("BILLING", "SUBSCRIPTION");
        ClinicUser doctor = ClinicUser.builder().email("d@zest.in").clinicId("C1").role("DOCTOR").invitedAt(LocalDateTime.now()).build();
        assertThat(service.me(doctor).isOwner()).isFalse();
        assertThat(service.me(doctor).getRoleLabel()).isEqualTo("Doctor");
        assertThat(service.me(doctor).getPages()).contains("PRESCRIPTIONS_WRITE").doesNotContain("BILLING", "STAFF");
    }
}
