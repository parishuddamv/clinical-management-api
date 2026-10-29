package com.clinicos.common.service;

import com.clinicos.common.dto.ForgotPasswordRequest;
import com.clinicos.common.dto.PasswordLoginRequest;
import com.clinicos.common.dto.ResetPasswordRequest;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.entity.PasswordResetToken;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.repository.ClinicUserRepository;
import com.clinicos.common.repository.PasswordResetTokenRepository;
import com.clinicos.common.security.JwtTokenProvider;
import com.clinicos.common.security.PasswordHashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordAuthServiceTest {

    private static final String PASSWORD = "Teal-river-42";
    private static final String HASH = PasswordHashing.encoder().encode(PASSWORD);

    private final ClinicUserRepository users = mock(ClinicUserRepository.class);
    private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    private final PasswordResetMailer mailer = mock(PasswordResetMailer.class);
    private final JwtTokenProvider jwt = mock(JwtTokenProvider.class);
    private PasswordAuthService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        ObjectProvider<ClinicUserRepository> userProvider = mock(ObjectProvider.class);
        ObjectProvider<PasswordResetTokenRepository> tokenProvider = mock(ObjectProvider.class);
        ObjectProvider<PasswordResetMailer> mailProvider = mock(ObjectProvider.class);
        when(userProvider.getObject()).thenReturn(users);
        when(tokenProvider.getObject()).thenReturn(tokens);
        when(mailProvider.getIfAvailable()).thenReturn(mailer);
        service = new PasswordAuthService(userProvider, tokenProvider, mailProvider, jwt);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://www.clinisysone.com/");
        when(jwt.generateToken(anyString(), any())).thenReturn("jwt");
    }

    private ClinicUser user(ClinicUser.UserStatus status) {
        return ClinicUser.builder().id(7L).email("owner@example.com").fullName("Asha Rao").role("ADMIN")
                .status(status).isActive(true).clinicId("CLINIC_1").passwordHash(HASH).failedLoginAttempts(0).build();
    }

    @Test
    void approvedUserSignsInWithClinicContext() {
        ClinicUser u = user(ClinicUser.UserStatus.APPROVED);
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(u));

        var response = service.login(new PasswordLoginRequest(" Owner@Example.com ", PASSWORD));

        assertThat(response.getToken()).isEqualTo("jwt");
        assertThat(response.getIsApproved()).isTrue();
        assertThat(response.getUser().getClinicId()).isEqualTo("CLINIC_1");
        verify(jwt).generateToken("owner@example.com", "CLINIC_1");
        assertThat(u.getLastLogin()).isNotNull();
    }

    @Test
    void pendingUserGetsIdentityOnlyToken() {
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user(ClinicUser.UserStatus.PENDING)));

        var response = service.login(new PasswordLoginRequest("owner@example.com", PASSWORD));

        assertThat(response.getIsApproved()).isFalse();
        verify(jwt).generateToken("owner@example.com", null);
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameError() {
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user(ClinicUser.UserStatus.APPROVED)));
        when(users.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        var wrong = catchThrowableOfType(() -> service.login(new PasswordLoginRequest("owner@example.com", "Wrong-pass-99")), RegistrationException.class);
        var unknown = catchThrowableOfType(() -> service.login(new PasswordLoginRequest("nobody@example.com", PASSWORD)), RegistrationException.class);

        assertThat(wrong.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknown.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrong.getMessage()).isEqualTo(unknown.getMessage());
        verify(jwt, never()).generateToken(anyString(), any());
    }

    @Test
    void googleOnlyAccountCannotUsePasswordSignIn() {
        ClinicUser u = user(ClinicUser.UserStatus.APPROVED);
        u.setPasswordHash(null);
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.login(new PasswordLoginRequest("owner@example.com", PASSWORD)))
                .isInstanceOfSatisfying(RegistrationException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void fiveFailuresLockTheAccount() {
        ClinicUser u = user(ClinicUser.UserStatus.APPROVED);
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(u));
        for (int i = 0; i < PasswordAuthService.MAX_FAILED_ATTEMPTS; i++) {
            assertThatThrownBy(() -> service.login(new PasswordLoginRequest("owner@example.com", "Wrong-pass-99")))
                    .isInstanceOf(RegistrationException.class);
        }
        assertThat(u.getLockedUntil()).isAfter(LocalDateTime.now());

        assertThatThrownBy(() -> service.login(new PasswordLoginRequest("owner@example.com", PASSWORD)))
                .isInstanceOfSatisfying(RegistrationException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.LOCKED));
    }

    @Test
    void forgotPasswordEmailsAOneTimeLinkAndStoresOnlyItsHash() {
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user(ClinicUser.UserStatus.APPROVED)));

        service.requestReset(new ForgotPasswordRequest("owner@example.com"), "10.0.0.1");

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendResetLink(eq("owner@example.com"), eq("Asha Rao"), url.capture(), eq(30));
        assertThat(url.getValue()).startsWith("https://www.clinisysone.com/reset-password?token=");
        String token = url.getValue().substring(url.getValue().indexOf("token=") + 6);

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(PasswordAuthService.sha256(token)).isNotEqualTo(token);
        verify(tokens).retireOutstanding(eq(7L), any());
    }

    @Test
    void forgotPasswordForUnknownEmailSendsNothingButDoesNotFail() {
        when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThatCode(() -> service.requestReset(new ForgotPasswordRequest("nobody@example.com"), null)).doesNotThrowAnyException();
        verifyNoInteractions(mailer);
    }

    @Test
    void forgotPasswordIsRateLimited() {
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user(ClinicUser.UserStatus.APPROVED)));
        when(tokens.countByUserIdAndCreatedAtAfter(eq(7L), any())).thenReturn((long) PasswordAuthService.MAX_RESETS_PER_HOUR);

        service.requestReset(new ForgotPasswordRequest("owner@example.com"), null);

        verifyNoInteractions(mailer);
    }

    @Test
    void resetSetsNewPasswordAndUsesUpTheToken() {
        ClinicUser u = user(ClinicUser.UserStatus.APPROVED);
        u.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        PasswordResetToken reset = PasswordResetToken.builder().userId(7L).tokenHash(PasswordAuthService.sha256("tok"))
                .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
        when(tokens.findByTokenHash(PasswordAuthService.sha256("tok"))).thenReturn(Optional.of(reset));
        when(users.findById(7L)).thenReturn(Optional.of(u));

        service.resetPassword(new ResetPasswordRequest("tok", "Brand-new-77", "Brand-new-77"));

        assertThat(PasswordHashing.encoder().matches("Brand-new-77", u.getPasswordHash())).isTrue();
        assertThat(u.getLockedUntil()).isNull();
        assertThat(reset.getUsedAt()).isNotNull();
    }

    @Test
    void expiredOrUsedTokensAreRejected() {
        PasswordResetToken expired = PasswordResetToken.builder().userId(7L).tokenHash(PasswordAuthService.sha256("old"))
                .expiresAt(LocalDateTime.now().minusMinutes(1)).build();
        when(tokens.findByTokenHash(PasswordAuthService.sha256("old"))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("old", "Brand-new-77", "Brand-new-77")))
                .isInstanceOfSatisfying(RegistrationException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(users, never()).save(any());
    }

    @Test
    void resetEnforcesThePasswordPolicy() {
        PasswordResetToken reset = PasswordResetToken.builder().userId(7L).tokenHash(PasswordAuthService.sha256("tok"))
                .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
        when(tokens.findByTokenHash(PasswordAuthService.sha256("tok"))).thenReturn(Optional.of(reset));
        when(users.findById(7L)).thenReturn(Optional.of(user(ClinicUser.UserStatus.APPROVED)));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("tok", "12345678", "12345678")))
                .hasMessageContaining("10 characters");
        assertThat(reset.getUsedAt()).isNull();
    }
}
