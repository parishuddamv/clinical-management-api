package com.clinicos.subscription.repository;

import com.clinicos.subscription.entity.SubscriptionFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionFeatureRepository extends JpaRepository<SubscriptionFeature, Long> {

    List<SubscriptionFeature> findByPlanIdOrderByIdAsc(Long planId);
}
