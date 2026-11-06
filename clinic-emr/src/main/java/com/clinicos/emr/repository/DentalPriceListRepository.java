package com.clinicos.emr.repository;

import com.clinicos.emr.entity.DentalPriceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DentalPriceListRepository extends JpaRepository<DentalPriceList, String> {
}
