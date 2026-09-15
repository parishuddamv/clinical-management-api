package com.clinicos.staff.service;

import com.clinicos.staff.dto.StaffMemberDTO;
import com.clinicos.staff.entity.StaffMember;
import com.clinicos.staff.entity.StaffMember.StaffRole;
import com.clinicos.staff.entity.StaffMember.StaffStatus;
import com.clinicos.staff.repository.RolePermissionRepository;
import com.clinicos.staff.repository.StaffMemberRepository;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffMemberRepository staffRepository;
    private final RolePermissionRepository permissionRepository;
    private final SubscriptionLimitGuardService subscriptionLimitGuardService;

    @Transactional
    public StaffMemberDTO createStaff(String clinicId, StaffMemberDTO dto) {
        log.info("Creating staff member: {} in clinic: {}", dto.getEmail(), clinicId);

        if (staffRepository.existsByEmailAndClinicId(dto.getEmail(), clinicId)) {
            throw new IllegalArgumentException("Staff with email " + dto.getEmail() + " already exists");
        }

        subscriptionLimitGuardService.validateCreateStaffWithinPlan(clinicId, dto.getRole());

        StaffMember staff = StaffMember.builder()
                .email(dto.getEmail())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .phone(dto.getPhone())
                .profileImageUrl(dto.getProfileImageUrl())
                .role(dto.getRole())
                .status(dto.getStatus() != null ? dto.getStatus() : StaffStatus.ACTIVE)
                .specialization(dto.getSpecialization())
                .qualification(dto.getQualification())
                .licenseNumber(dto.getLicenseNumber())
                .experienceYears(dto.getExperienceYears())
                .consultationFee(dto.getConsultationFee())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .pincode(dto.getPincode())
                .joinedDate(dto.getJoinedDate())
                .notes(dto.getNotes())
                .build();

        staff.setClinicId(clinicId);
        staff = staffRepository.save(staff);

        log.info("Created staff member with ID: {}", staff.getId());
        return mapToDTO(staff, clinicId);
    }

    @Transactional(readOnly = true)
    public StaffMemberDTO getStaff(String clinicId, Long staffId) {
        StaffMember staff = staffRepository.findByIdAndClinicId(staffId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));
        return mapToDTO(staff, clinicId);
    }

    @Transactional(readOnly = true)
    public StaffMemberDTO getStaffByEmail(String clinicId, String email) {
        StaffMember staff = staffRepository.findByEmailAndClinicId(email, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with email: " + email));
        return mapToDTO(staff, clinicId);
    }

    @Transactional
    public StaffMemberDTO updateStaff(String clinicId, Long staffId, StaffMemberDTO dto) {
        StaffMember staff = staffRepository.findByIdAndClinicId(staffId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));

        if (dto.getFirstName() != null) staff.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) staff.setLastName(dto.getLastName());
        if (dto.getPhone() != null) staff.setPhone(dto.getPhone());
        if (dto.getProfileImageUrl() != null) staff.setProfileImageUrl(dto.getProfileImageUrl());
        if (dto.getRole() != null) staff.setRole(dto.getRole());
        if (dto.getStatus() != null) staff.setStatus(dto.getStatus());
        if (dto.getSpecialization() != null) staff.setSpecialization(dto.getSpecialization());
        if (dto.getQualification() != null) staff.setQualification(dto.getQualification());
        if (dto.getLicenseNumber() != null) staff.setLicenseNumber(dto.getLicenseNumber());
        if (dto.getExperienceYears() != null) staff.setExperienceYears(dto.getExperienceYears());
        if (dto.getConsultationFee() != null) staff.setConsultationFee(dto.getConsultationFee());
        if (dto.getAddress() != null) staff.setAddress(dto.getAddress());
        if (dto.getCity() != null) staff.setCity(dto.getCity());
        if (dto.getState() != null) staff.setState(dto.getState());
        if (dto.getPincode() != null) staff.setPincode(dto.getPincode());
        if (dto.getNotes() != null) staff.setNotes(dto.getNotes());

        staff = staffRepository.save(staff);
        return mapToDTO(staff, clinicId);
    }

    @Transactional
    public void deactivateStaff(String clinicId, Long staffId) {
        StaffMember staff = staffRepository.findByIdAndClinicId(staffId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));
        staff.setStatus(StaffStatus.INACTIVE);
        staffRepository.save(staff);
        log.info("Deactivated staff: {}", staffId);
    }

    @Transactional(readOnly = true)
    public Page<StaffMemberDTO> getAllStaff(String clinicId, Pageable pageable) {
        return staffRepository.findByClinicIdOrderByFirstNameAsc(clinicId, pageable)
                .map(s -> mapToDTO(s, clinicId));
    }

    @Transactional(readOnly = true)
    public Page<StaffMemberDTO> getStaffByRole(String clinicId, StaffRole role, Pageable pageable) {
        return staffRepository.findByClinicIdAndRoleOrderByFirstNameAsc(clinicId, role, pageable)
                .map(s -> mapToDTO(s, clinicId));
    }

    @Transactional(readOnly = true)
    public List<StaffMemberDTO> getActiveDoctors(String clinicId) {
        return staffRepository.findActiveDoctors(clinicId).stream()
                .map(s -> mapToDTO(s, clinicId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<StaffMemberDTO> searchStaff(String clinicId, String query, Pageable pageable) {
        return staffRepository.searchStaff(clinicId, query, pageable)
                .map(s -> mapToDTO(s, clinicId));
    }

    @Transactional(readOnly = true)
    public Set<String> getPermissions(String clinicId, String role) {
        return permissionRepository.findPermissionsByRole(clinicId, role);
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(String clinicId, String role, String permission) {
        Set<String> permissions = getPermissions(clinicId, role);
        return permissions.contains(permission);
    }

    private StaffMemberDTO mapToDTO(StaffMember staff, String clinicId) {
        Set<String> permissions = permissionRepository.findPermissionsByRole(clinicId, staff.getRole().name());

        return StaffMemberDTO.builder()
                .id(staff.getId())
                .firstName(staff.getFirstName())
                .lastName(staff.getLastName())
                .fullName(staff.getFullName())
                .email(staff.getEmail())
                .phone(staff.getPhone())
                .profileImageUrl(staff.getProfileImageUrl())
                .role(staff.getRole())
                .status(staff.getStatus())
                .specialization(staff.getSpecialization())
                .qualification(staff.getQualification())
                .licenseNumber(staff.getLicenseNumber())
                .experienceYears(staff.getExperienceYears())
                .consultationFee(staff.getConsultationFee())
                .address(staff.getAddress())
                .city(staff.getCity())
                .state(staff.getState())
                .pincode(staff.getPincode())
                .joinedDate(staff.getJoinedDate())
                .notes(staff.getNotes())
                .permissions(permissions)
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }
}

