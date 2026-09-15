package com.clinicos.billing.subscription.repository;

import com.clinicos.billing.subscription.entity.ClinicSubscriptionAddon;
import com.clinicos.billing.subscription.model.AddonCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicSubscriptionAddonRepository extends JpaRepository<ClinicSubscriptionAddon, Long> {

    List<ClinicSubscriptionAddon> findByClinicIdAndActiveTrue(String clinicId);

    Optional<ClinicSubscriptionAddon> findByClinicIdAndAddonCode(String clinicId, AddonCode addonCode);
}

