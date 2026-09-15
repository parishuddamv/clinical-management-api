package com.clinicos.billing.subscription.repository;

import com.clinicos.billing.subscription.entity.PlanLimit;
import com.clinicos.billing.subscription.model.LimitKey;
import com.clinicos.billing.subscription.model.PlanCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlanLimitRepository extends JpaRepository<PlanLimit, Long> {

    @Query("SELECT pl FROM PlanLimit pl WHERE pl.plan.code = :planCode")
    List<PlanLimit> findByPlanCode(@Param("planCode") PlanCode planCode);

    @Query("SELECT pl FROM PlanLimit pl WHERE pl.plan.code = :planCode AND pl.limitKey = :limitKey")
    Optional<PlanLimit> findByPlanCodeAndLimitKey(@Param("planCode") PlanCode planCode, @Param("limitKey") LimitKey limitKey);
}

