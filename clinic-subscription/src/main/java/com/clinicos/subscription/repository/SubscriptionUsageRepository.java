package com.clinicos.subscription.repository;

import com.clinicos.subscription.entity.SubscriptionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionUsageRepository extends JpaRepository<SubscriptionUsage, Long> {

    List<SubscriptionUsage> findByClinicSubscriptionIdOrderByIdAsc(Long clinicSubscriptionId);

    Optional<SubscriptionUsage> findByClinicSubscriptionIdAndCode(
            Long clinicSubscriptionId,
            String code
    );

    boolean existsByClinicSubscriptionIdAndCode(
            Long clinicSubscriptionId,
            String code
    );
}