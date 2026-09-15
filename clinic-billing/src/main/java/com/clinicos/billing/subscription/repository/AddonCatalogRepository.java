package com.clinicos.billing.subscription.repository;

import com.clinicos.billing.subscription.entity.AddonCatalog;
import com.clinicos.billing.subscription.model.AddonCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddonCatalogRepository extends JpaRepository<AddonCatalog, Long> {

    Optional<AddonCatalog> findByCode(AddonCode code);

    List<AddonCatalog> findByActiveTrueOrderByIdAsc();
}

