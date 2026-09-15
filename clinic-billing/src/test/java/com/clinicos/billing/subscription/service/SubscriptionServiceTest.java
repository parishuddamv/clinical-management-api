package com.clinicos.billing.subscription.service;

import com.clinicos.billing.subscription.config.SubscriptionProperties;
import com.clinicos.billing.subscription.dto.CurrentSubscriptionDto;
import com.clinicos.billing.subscription.dto.FeatureAvailabilityDto;
import com.clinicos.billing.subscription.entity.ClinicSubscription;
import com.clinicos.billing.subscription.entity.ClinicSubscriptionAddon;
import com.clinicos.billing.subscription.model.*;
import com.clinicos.billing.subscription.repository.*;
import com.clinicos.common.exception.ClinicAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private ClinicSubscriptionRepository clinicSubscriptionRepository;
    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;
    @Mock
    private PlanFeatureRepository planFeatureRepository;
    @Mock
    private PlanLimitRepository planLimitRepository;
    @Mock
    private AddonCatalogRepository addonCatalogRepository;
    @Mock
    private ClinicSubscriptionAddonRepository clinicSubscriptionAddonRepository;
    @Mock
    private SubscriptionAuditService subscriptionAuditService;
    @Mock
    private UsageMeterService usageMeterService;
    @Mock
    private RolePermissionService rolePermissionService;
    @Mock
    private Authentication authentication;

    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        SubscriptionProperties props = new SubscriptionProperties();
        props.setTrialDays(30);
        props.setGraceDays(7);

        subscriptionService = new SubscriptionService(
                clinicSubscriptionRepository,
                subscriptionPlanRepository,
                planFeatureRepository,
                planLimitRepository,
                addonCatalogRepository,
                clinicSubscriptionAddonRepository,
                subscriptionAuditService,
                props,
                usageMeterService,
                rolePermissionService
        );
    }

    @Test
    void getCurrentSubscription_createsProfessionalTrialForNewClinic() {
        ActorContext actor = ActorContext.builder()
                .email("owner@demo.com")
                .clinicId("CLINIC_A")
                .role("ADMIN")
                .superAdmin(false)
                .build();

        when(rolePermissionService.resolveActor(authentication)).thenReturn(actor);
        when(clinicSubscriptionRepository.findByClinicId("CLINIC_A")).thenReturn(Optional.empty());
        when(clinicSubscriptionRepository.save(any(ClinicSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(clinicSubscriptionAddonRepository.findByClinicIdAndActiveTrue("CLINIC_A")).thenReturn(List.of());

        CurrentSubscriptionDto dto = subscriptionService.getCurrentSubscription(authentication);

        assertEquals(PlanCode.PROFESSIONAL, dto.getPlanCode());
        assertEquals(SubscriptionStatus.TRIAL, dto.getStatus());
        assertEquals(LocalDate.now().plusDays(30), dto.getTrialEndDate());
        verify(subscriptionAuditService, times(1))
                .record(eq("CLINIC_A"), eq(SubscriptionAction.SUBSCRIPTION_CREATED), eq("owner@demo.com"), eq("ADMIN"), anyString());
    }

    @Test
    void checkFeature_allowsFeatureThroughAddon() {
        ActorContext actor = ActorContext.builder()
                .email("owner@demo.com")
                .clinicId("CLINIC_A")
                .role("ADMIN")
                .superAdmin(false)
                .build();

        ClinicSubscription existing = ClinicSubscription.builder()
                .planCode(PlanCode.STARTER)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(25))
                .renewalDate(LocalDate.now().plusDays(25))
                .build();
        existing.setClinicId("CLINIC_A");

        ClinicSubscriptionAddon pharmacyAddon = ClinicSubscriptionAddon.builder()
                .addonCode(AddonCode.PHARMACY)
                .active(true)
                .startDate(LocalDate.now().minusDays(1))
                .build();
        pharmacyAddon.setClinicId("CLINIC_A");

        when(rolePermissionService.resolveActor(authentication)).thenReturn(actor);
        when(clinicSubscriptionRepository.findByClinicId("CLINIC_A")).thenReturn(Optional.of(existing));
        when(planFeatureRepository.findFeatureCodesByPlan(PlanCode.STARTER)).thenReturn(Set.of(FeatureCode.BASIC_BILLING));
        when(clinicSubscriptionAddonRepository.findByClinicIdAndActiveTrue("CLINIC_A")).thenReturn(List.of(pharmacyAddon));

        FeatureAvailabilityDto result = subscriptionService.checkFeature(authentication, FeatureCode.PHARMACY);

        assertTrue(result.isAvailable());
        assertEquals("Feature is available", result.getReason());
    }

    @Test
    void enforceAccess_deniesSuspendedSubscription() {
        ActorContext actor = ActorContext.builder()
                .email("owner@demo.com")
                .clinicId("CLINIC_A")
                .role("ADMIN")
                .superAdmin(false)
                .build();

        ClinicSubscription existing = ClinicSubscription.builder()
                .planCode(PlanCode.PROFESSIONAL)
                .status(SubscriptionStatus.SUSPENDED)
                .billingCycle(BillingCycle.MONTHLY)
                .startDate(LocalDate.now().minusDays(35))
                .endDate(LocalDate.now().minusDays(1))
                .renewalDate(LocalDate.now().minusDays(1))
                .build();
        existing.setClinicId("CLINIC_A");

        when(rolePermissionService.resolveActor(authentication)).thenReturn(actor);
        when(clinicSubscriptionRepository.findByClinicId("CLINIC_A")).thenReturn(Optional.of(existing));

        assertThrows(ClinicAccessDeniedException.class,
                () -> subscriptionService.enforceAccess(authentication, FeatureCode.BASIC_BILLING, "VIEW_BILLING"));

        verify(rolePermissionService, never()).requirePermission(anyString(), anyString(), anyString());
    }
}

