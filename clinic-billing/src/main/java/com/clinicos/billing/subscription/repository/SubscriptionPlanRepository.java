package com.clinicos.billing.subscription.repository;

import com.clinicos.billing.subscription.entity.SubscriptionPlan;
import com.clinicos.billing.subscription.model.PlanCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findByCode(PlanCode code);

    List<SubscriptionPlan> findByActiveTrueOrderByIdAsc();
}

