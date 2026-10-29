package com.clinicos.common.repository;

import com.clinicos.common.entity.ClinicProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClinicProfileRepository extends JpaRepository<ClinicProfile, String> {

    Optional<ClinicProfile> findByBookingSlug(String bookingSlug);
}
