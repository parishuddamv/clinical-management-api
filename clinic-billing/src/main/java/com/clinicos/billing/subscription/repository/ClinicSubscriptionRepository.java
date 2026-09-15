package com.clinicos.billing.subscription.repository;

import com.clinicos.billing.subscription.entity.ClinicSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClinicSubscriptionRepository extends JpaRepository<ClinicSubscription, Long> {

    Optional<ClinicSubscription> findByClinicId(String clinicId);
}

