package com.clinicos.emr.repository;

import com.clinicos.emr.entity.FavouriteDrug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavouriteDrugRepository extends JpaRepository<FavouriteDrug, Long> {

    List<FavouriteDrug> findTop50ByClinicIdAndDoctorUserOrderByUseCountDescDrugNameAsc(String clinicId, String doctorUser);

    Optional<FavouriteDrug> findByIdAndClinicIdAndDoctorUser(Long id, String clinicId, String doctorUser);

    @Query("SELECT f FROM FavouriteDrug f WHERE f.clinicId = :clinicId AND f.doctorUser = :doctor "
            + "AND LOWER(f.drugName) = LOWER(:drugName) AND COALESCE(LOWER(f.strength), '') = COALESCE(LOWER(:strength), '')")
    Optional<FavouriteDrug> findSame(@Param("clinicId") String clinicId, @Param("doctor") String doctor,
                                     @Param("drugName") String drugName, @Param("strength") String strength);

    long countByClinicIdAndDoctorUser(String clinicId, String doctorUser);
}
