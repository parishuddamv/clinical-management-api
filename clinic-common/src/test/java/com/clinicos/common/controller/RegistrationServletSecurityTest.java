package com.clinicos.common.controller;

import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.exception.GlobalExceptionHandler;
import com.clinicos.common.repository.ClinicUserRepository;
import com.clinicos.common.security.CommonSecurityConfig;
import com.clinicos.common.security.JwtTokenProvider;
import com.clinicos.common.security.RegistrationAccess;
import com.clinicos.common.service.UserRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(RegistrationServletSecurityTest.Config.class)
@WebAppConfiguration
class RegistrationServletSecurityTest {
    @Configuration
    @EnableWebMvc
    @Import({UserRegistrationController.class, CommonSecurityConfig.class, RegistrationAccess.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean com.fasterxml.jackson.databind.ObjectMapper objectMapper() { return new com.fasterxml.jackson.databind.ObjectMapper(); }
        @Bean ClinicUserRepository users() { return mock(ClinicUserRepository.class); }
        @Bean UserRegistrationService service() { return mock(UserRegistrationService.class); }
        @Bean JwtTokenProvider tokens() {
            var tokens = new JwtTokenProvider();
            ReflectionTestUtils.setField(tokens, "jwtSecret", "test-only-secret-abcdefghijklmnopqrstuvwxyz-1234567890-abcdefghijklmnop");
            ReflectionTestUtils.setField(tokens, "jwtExpirationMs", 60000L);
            return tokens;
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired ClinicUserRepository users;
    @Autowired UserRegistrationService service;
    @Autowired JwtTokenProvider tokens;
    MockMvc http;

    @BeforeEach void setup() {
        reset(users, service);
        http = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
    }

    @Test void anonymousAndNormalAdminCannotUseAnyManagementEndpoint() throws Exception {
        var admin = ClinicUser.builder().email("admin@example.com").role("ADMIN")
                .status(ClinicUser.UserStatus.APPROVED).isActive(true).isSuperAdmin(false).build();
        when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        String token = "Bearer " + tokens.generateToken("admin@example.com", "A");
        for (String operation : new String[] {"approve", "reject", "suspend", "reactivate", "review"}) {
            String path = "/api/v1/auth/admin/" + operation + "/target@example.com";
            http.perform(put(path)).andExpect(status().isForbidden());
            http.perform(put(path).header("Authorization", token)).andExpect(status().isForbidden());
        }
        http.perform(get("/api/v1/auth/admin/registrations").header("Authorization", token)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void superAdminCanCallExistingApprovalAndSpoofedActorIsNotForwarded() throws Exception {
        var root = ClinicUser.builder().email("root@example.com").role("SUPER_ADMIN")
                .status(ClinicUser.UserStatus.APPROVED).isActive(true).isSuperAdmin(true).build();
        when(users.findByEmailIgnoreCase("root@example.com")).thenReturn(Optional.of(root));
        String token = "Bearer " + tokens.generateToken("root@example.com", "A");
        http.perform(put("/api/v1/auth/admin/approve/target@example.com")
                        .param("approvedBy", "spoof@example.com").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(service).approveUser("target@example.com", null, token);
    }
}
