package com.clinicos.common.repository;

import com.clinicos.common.entity.RegistrationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationHistoryRepository extends JpaRepository<RegistrationHistory, Long> {
    Page<RegistrationHistory> findByUserIdOrderByPerformedAtDescIdDesc(Long userId, Pageable pageable);
}
