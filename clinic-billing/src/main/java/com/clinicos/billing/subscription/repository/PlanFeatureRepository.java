package com.clinicos.billing.subscription.repository;

import com.clinicos.billing.subscription.entity.PlanFeature;
import com.clinicos.billing.subscription.model.FeatureCode;
import com.clinicos.billing.subscription.model.PlanCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.Optional;

public interface PlanFeatureRepository extends JpaRepository<PlanFeature, Long> {

    @Query("SELECT pf.featureCode FROM PlanFeature pf WHERE pf.plan.code = :planCode")
    Set<FeatureCode> findFeatureCodesByPlan(@Param("planCode") PlanCode planCode);

    @Query("SELECT pf FROM PlanFeature pf WHERE pf.plan.code = :planCode AND pf.featureCode = :featureCode")
    Optional<PlanFeature> findByPlanCodeAndFeatureCode(@Param("planCode") PlanCode planCode, @Param("featureCode") FeatureCode featureCode);
}

