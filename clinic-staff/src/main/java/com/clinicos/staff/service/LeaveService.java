package com.clinicos.staff.service;

import com.clinicos.staff.dto.StaffLeaveDTO;
import com.clinicos.staff.entity.ScheduleOverride;
import com.clinicos.staff.entity.StaffLeave;
import com.clinicos.staff.entity.StaffLeave.LeaveStatus;
import com.clinicos.staff.entity.StaffMember;
import com.clinicos.staff.repository.ScheduleOverrideRepository;
import com.clinicos.staff.repository.StaffLeaveRepository;
import com.clinicos.staff.repository.StaffMemberRepository;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final StaffLeaveRepository leaveRepository;
    private final StaffMemberRepository staffRepository;
    private final ScheduleOverrideRepository overrideRepository;

    @Transactional
    public StaffLeaveDTO applyLeave(String clinicId, StaffLeaveDTO dto) {
        StaffMember staff = staffRepository.findByIdAndClinicId(dto.getStaffId(), clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + dto.getStaffId()));

        StaffLeave leave = StaffLeave.builder()
                .staff(staff)
                .leaveType(dto.getLeaveType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .status(LeaveStatus.PENDING)
                .build();

        leave.setClinicId(clinicId);
        leave = leaveRepository.save(leave);

        log.info("Leave applied by staff: {} from {} to {}", dto.getStaffId(), dto.getStartDate(), dto.getEndDate());
        return mapToDTO(leave);
    }

    @Transactional
    public StaffLeaveDTO approveLeave(String clinicId, Long leaveId, Long approverId) {
        StaffLeave leave = leaveRepository.findByIdAndClinicId(leaveId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found: " + leaveId));

        StaffMember approver = staffRepository.findByIdAndClinicId(approverId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found: " + approverId));

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedBy(approver);
        leave.setApprovedAt(LocalDateTime.now());
        leave = leaveRepository.save(leave);

        // Create schedule overrides for the leave period
        createLeaveOverrides(leave);

        log.info("Leave approved: {} by: {}", leaveId, approverId);
        return mapToDTO(leave);
    }

    @Transactional
    public StaffLeaveDTO rejectLeave(String clinicId, Long leaveId, Long approverId, String reason) {
        StaffLeave leave = leaveRepository.findByIdAndClinicId(leaveId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found: " + leaveId));

        StaffMember approver = staffRepository.findByIdAndClinicId(approverId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found: " + approverId));

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setApprovedBy(approver);
        leave.setApprovedAt(LocalDateTime.now());
        leave.setRejectionReason(reason);
        leave = leaveRepository.save(leave);

        log.info("Leave rejected: {} by: {}", leaveId, approverId);
        return mapToDTO(leave);
    }

    @Transactional
    public void cancelLeave(String clinicId, Long leaveId) {
        StaffLeave leave = leaveRepository.findByIdAndClinicId(leaveId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found: " + leaveId));

        if (leave.getStatus() == LeaveStatus.APPROVED) {
            // Remove schedule overrides
            List<ScheduleOverride> overrides = overrideRepository.findByStaffIdAndOverrideDateBetweenOrderByOverrideDateAsc(
                    leave.getStaff().getId(), leave.getStartDate(), leave.getEndDate());
            overrideRepository.deleteAll(overrides);
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        leaveRepository.save(leave);
        log.info("Leave cancelled: {}", leaveId);
    }

    @Transactional(readOnly = true)
    public Page<StaffLeaveDTO> getStaffLeaves(String clinicId, Long staffId, Pageable pageable) {
        staffRepository.findByIdAndClinicId(staffId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));

        return leaveRepository.findByStaffIdOrderByStartDateDesc(staffId, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<StaffLeaveDTO> getPendingLeaves(String clinicId, Pageable pageable) {
        return leaveRepository.findByClinicIdAndStatusOrderByStartDateDesc(clinicId, LeaveStatus.PENDING, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public List<StaffLeaveDTO> getUpcomingLeaves(String clinicId, LocalDate startDate, LocalDate endDate) {
        return leaveRepository.findApprovedLeavesInRange(clinicId, startDate, endDate)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private void createLeaveOverrides(StaffLeave leave) {
        LocalDate current = leave.getStartDate();
        while (!current.isAfter(leave.getEndDate())) {
            if (!overrideRepository.existsByStaffIdAndOverrideDate(leave.getStaff().getId(), current)) {
                ScheduleOverride override = ScheduleOverride.builder()
                        .staff(leave.getStaff())
                        .overrideDate(current)
                        .overrideType(ScheduleOverride.OverrideType.LEAVE)
                        .reason(leave.getLeaveType().name() + " Leave")
                        .isFullDay(true)
                        .build();
                override.setClinicId(leave.getClinicId());
                overrideRepository.save(override);
            }
            current = current.plusDays(1);
        }
    }

    private StaffLeaveDTO mapToDTO(StaffLeave leave) {
        return StaffLeaveDTO.builder()
                .id(leave.getId())
                .staffId(leave.getStaff().getId())
                .staffName(leave.getStaff().getFullName())
                .leaveType(leave.getLeaveType())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .reason(leave.getReason())
                .status(leave.getStatus())
                .approvedById(leave.getApprovedBy() != null ? leave.getApprovedBy().getId() : null)
                .approvedByName(leave.getApprovedBy() != null ? leave.getApprovedBy().getFullName() : null)
                .approvedAt(leave.getApprovedAt())
                .rejectionReason(leave.getRejectionReason())
                .days(leave.getDays())
                .createdAt(leave.getCreatedAt())
                .build();
    }
}

