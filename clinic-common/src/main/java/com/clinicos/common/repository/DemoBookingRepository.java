package com.clinicos.common.repository;

import com.clinicos.common.entity.DemoBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DemoBookingRepository extends JpaRepository<DemoBooking, Long> {

    Page<DemoBooking> findByStatus(
        DemoBooking.DemoBookingStatus status,
        Pageable pageable
    );

    List<DemoBooking> findByDemoDateOrderByDemoTimeAsc(LocalDate demoDate);

    Page<DemoBooking> findByStatusAndDemoDate(
        DemoBooking.DemoBookingStatus status,
        LocalDate demoDate,
        Pageable pageable
    );
}
