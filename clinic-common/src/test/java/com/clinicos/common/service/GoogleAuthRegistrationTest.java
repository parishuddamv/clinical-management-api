package com.clinicos.common.service;

import com.clinicos.common.dto.GoogleAuthRequest;
import com.clinicos.common.dto.GoogleUserInfo;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.repository.ClinicUserRepository;
import com.clinicos.common.security.GoogleIdentityVerifier;
import com.clinicos.common.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class GoogleAuthRegistrationTest {
    private final JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    private final GoogleIdentityVerifier verifier = mock(GoogleIdentityVerifier.class);
    private final ClinicUserRepository users = mock(ClinicUserRepository.class);
    private GoogleAuthService service;
    private final GoogleAuthRequest request = GoogleAuthRequest.builder()
            .idToken("google-token").clinicId("BROWSER_CLINIC").build();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        ObjectProvider<ClinicUserRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(users);
        service = new GoogleAuthService(tokens, new ObjectMapper(), provider, verifier);
        when(verifier.verify("google-token")).thenReturn(GoogleUserInfo.builder()
                .email("Person@Example.com").sub("google-user").emailVerified(true).build());
    }

    @ParameterizedTest
    @EnumSource(ClinicUser.UserStatus.class)
    void clinicContextDependsOnCurrentRegistrationStatus(ClinicUser.UserStatus status) {
        var user = ClinicUser.builder().id(10L).email("person@example.com").status(status)
                .clinicId("SERVER_CLINIC").isActive(true).build();
        when(users.findByEmail("person@example.com")).thenReturn(Optional.of(user));
        var response = service.authenticateWithGoogle(request);
        String expectedClinic = status == ClinicUser.UserStatus.APPROVED ? "SERVER_CLINIC" : null;
        assertThat(response.getClinicId()).isEqualTo(expectedClinic);
        verify(tokens).generateToken("person@example.com", expectedClinic);
        verify(users).recordLogin(eq(10L), any());
        verify(users, never()).save(any());
    }

    @Test void unregisteredIdentityCannotSelectClinic() {
        when(users.findByEmail("person@example.com")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.empty());
        assertThat(service.authenticateWithGoogle(request).getClinicId()).isNull();
        verify(tokens).generateToken("person@example.com", null);
    }

    @Test void invalidGoogleIdentityCannotIssueApplicationJwt() {
        when(verifier.verify("google-token")).thenThrow(new RegistrationException(HttpStatus.UNAUTHORIZED, "Invalid ID token."));
        assertThatThrownBy(() -> service.authenticateWithGoogle(request)).isInstanceOf(RegistrationException.class);
        verifyNoInteractions(tokens, users);
    }
}
