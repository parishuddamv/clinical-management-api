package com.clinicos.followup.service;

import com.clinicos.followup.dto.CreateFollowUpRequest;
import com.clinicos.followup.dto.FollowUpResponse;
import com.clinicos.followup.dto.UpdateFollowUpRequest;
import com.clinicos.followup.entity.FollowUp;
import com.clinicos.followup.entity.FollowUp.FollowUpStatus;
import com.clinicos.followup.repository.FollowUpRepository;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final FollowUpRepository followUpRepository;

    @Transactional
    public FollowUpResponse create(String clinicId, CreateFollowUpRequest request) {
        FollowUp followUp = FollowUp.builder()
                .patientId(request.getPatientId())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .clinicalNotes(request.getClinicalNotes())
                .status(FollowUpStatus.PENDING)
                .build();
        followUp.setClinicId(clinicId);
        return FollowUpResponse.from(followUpRepository.save(followUp));
    }

    @Transactional(readOnly = true)
    public FollowUpResponse getById(String clinicId, Long id) {
        FollowUp followUp = findByIdAndClinic(clinicId, id);
        return FollowUpResponse.from(followUp);
    }

    @Transactional
    public FollowUpResponse update(String clinicId, Long id, UpdateFollowUpRequest request) {
        FollowUp followUp = findByIdAndClinic(clinicId, id);

        if (request.getStatus() != null) {
            followUp.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            followUp.setNotes(request.getNotes());
        }
        if (request.getClinicalNotes() != null) {
            followUp.setClinicalNotes(request.getClinicalNotes());
        }

        return FollowUpResponse.from(followUpRepository.save(followUp));
    }

    @Transactional(readOnly = true)
    public Page<FollowUpResponse> getAll(String clinicId, FollowUpStatus status, Pageable pageable) {
        Page<FollowUp> page = (status != null)
                ? followUpRepository.findByClinicIdAndStatus(clinicId, status, pageable)
                : followUpRepository.findByClinicId(clinicId, pageable);
        return page.map(FollowUpResponse::from);
    }

    /**
     * Get pending follow-ups for the clinic
     */
    @Transactional(readOnly = true)
    public Page<FollowUpResponse> getPendingFollowUps(String clinicId, Pageable pageable) {
        log.info("Fetching pending follow-ups for clinic: {}", clinicId);
        Page<FollowUp> page = followUpRepository.findPendingByClinicId(clinicId, pageable);
        return page.map(FollowUpResponse::from);
    }

    /**
     * Get overdue follow-ups for the clinic (pending and past due date)
     */
    @Transactional(readOnly = true)
    public Page<FollowUpResponse> getOverdueFollowUps(String clinicId, Pageable pageable) {
        LocalDate today = LocalDate.now();
        log.info("Fetching overdue follow-ups for clinic: {} (before {})", clinicId, today);
        Page<FollowUp> page = followUpRepository.findOverdueByClinicId(clinicId, today, pageable);
        return page.map(FollowUpResponse::from);
    }

    private FollowUp findByIdAndClinic(String clinicId, Long id) {
        return followUpRepository.findById(id)
                .filter(f -> clinicId.equals(f.getClinicId()))
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up not found: " + id));
    }
}
