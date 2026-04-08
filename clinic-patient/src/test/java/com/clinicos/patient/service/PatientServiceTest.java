package com.clinicos.patient.service;

import com.clinicos.patient.dto.*;
import com.clinicos.patient.entity.Patient;
import com.clinicos.patient.exception.DuplicatePatientException;
import com.clinicos.patient.exception.PatientNotFoundException;
import com.clinicos.patient.repository.PatientRepository;
import com.clinicos.patient.repository.PatientTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PatientService
 */
@DisplayName("PatientService Unit Tests")
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientTagRepository patientTagRepository;

    @InjectMocks
    private PatientService patientService;

    private String clinicId = "CLINIC_001";
    private RegisterPatientRequest registerRequest;
    private Patient testPatient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize test data
        registerRequest = RegisterPatientRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .phone("9876543210")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .gender(Patient.Gender.MALE)
                .bloodGroup("O+")
                .address("123 Main St")
                .emergencyContactName("Jane Doe")
                .emergencyContactPhone("9876543211")
                .build();

        testPatient = new Patient();
        testPatient.setId(1L);
        testPatient.setClinicId(clinicId);
        testPatient.setFirstName("John");
        testPatient.setLastName("Doe");
        testPatient.setPhone("9876543210");
        testPatient.setDateOfBirth(LocalDate.of(1990, 1, 15));
        testPatient.setGender(Patient.Gender.MALE);
        testPatient.setBloodGroup("O+");
        testPatient.setAddress("123 Main St");
        testPatient.setEmergencyContactName("Jane Doe");
        testPatient.setEmergencyContactPhone("9876543211");
        testPatient.setIsActive(true);
    }

    @Test
    @DisplayName("should register new patient successfully")
    void testRegisterPatientSuccess() {
        // Arrange
        when(patientRepository.findByClinicIdAndPhone(clinicId, registerRequest.getPhone()))
                .thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class)))
                .thenReturn(testPatient);

        // Act
        PatientResponse response = patientService.registerPatient(clinicId, registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("9876543210", response.getPhone());
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("should throw DuplicatePatientException when registering with duplicate phone")
    void testRegisterPatientWithDuplicatePhone() {
        // Arrange
        when(patientRepository.findByClinicIdAndPhone(clinicId, registerRequest.getPhone()))
                .thenReturn(Optional.of(testPatient));

        // Act & Assert
        assertThrows(DuplicatePatientException.class, () ->
                patientService.registerPatient(clinicId, registerRequest)
        );
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("should throw PatientNotFoundException when getting non-existent patient")
    void testGetPatientNotFound() {
        // Arrange
        when(patientRepository.findByIdAndClinicId(1L, clinicId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PatientNotFoundException.class, () ->
                patientService.getPatientById(clinicId, 1L)
        );
    }

    @Test
    @DisplayName("should get patient by ID successfully")
    void testGetPatientByIdSuccess() {
        // Arrange
        when(patientRepository.findByIdAndClinicId(1L, clinicId))
                .thenReturn(Optional.of(testPatient));
        when(patientTagRepository.findByClinicIdAndPatientId(clinicId, 1L))
                .thenReturn(List.of());

        // Act
        PatientResponse response = patientService.getPatientById(clinicId, 1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("John", response.getFirstName());
    }

    @Test
    @DisplayName("should update patient successfully")
    void testUpdatePatientSuccess() {
        // Arrange
        UpdatePatientRequest updateRequest = UpdatePatientRequest.builder()
                .firstName("Johnny")
                .build();

        when(patientRepository.findByIdAndClinicId(1L, clinicId))
                .thenReturn(Optional.of(testPatient));
        when(patientRepository.save(any(Patient.class)))
                .thenReturn(testPatient);
        when(patientTagRepository.findByClinicIdAndPatientId(clinicId, 1L))
                .thenReturn(List.of());

        // Act
        PatientResponse response = patientService.updatePatient(clinicId, 1L, updateRequest);

        // Assert
        assertNotNull(response);
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("should throw DuplicatePatientException when updating to duplicate phone")
    void testUpdatePatientWithDuplicatePhone() {
        // Arrange
        UpdatePatientRequest updateRequest = UpdatePatientRequest.builder()
                .phone("9876543212")
                .build();

        when(patientRepository.findByIdAndClinicId(1L, clinicId))
                .thenReturn(Optional.of(testPatient));
        when(patientRepository.findByClinicIdAndPhone(clinicId, "9876543212"))
                .thenReturn(Optional.of(testPatient));

        // Act & Assert
        assertThrows(DuplicatePatientException.class, () ->
                patientService.updatePatient(clinicId, 1L, updateRequest)
        );
    }

    @Test
    @DisplayName("should soft delete patient successfully")
    void testSoftDeletePatient() {
        // Arrange
        when(patientRepository.findByIdAndClinicId(1L, clinicId))
                .thenReturn(Optional.of(testPatient));
        when(patientRepository.save(any(Patient.class)))
                .thenReturn(testPatient);

        // Act
        patientService.softDeletePatient(clinicId, 1L);

        // Assert
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("should add tag to patient successfully")
    void testAddTagSuccess() {
        // Arrange
        when(patientRepository.findByIdAndClinicId(1L, clinicId))
                .thenReturn(Optional.of(testPatient));
        when(patientTagRepository.existsByClinicIdAndPatientIdAndTag(clinicId, 1L, "diabetic"))
                .thenReturn(false);

        // Act
        patientService.addTag(clinicId, 1L, "diabetic");

        // Assert
        verify(patientTagRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("should not add duplicate tag")
    void testAddDuplicateTag() {
        // Arrange
        when(patientRepository.findByIdAndClinicId(1L, clinicId))
                .thenReturn(Optional.of(testPatient));
        when(patientTagRepository.existsByClinicIdAndPatientIdAndTag(clinicId, 1L, "diabetic"))
                .thenReturn(true);

        // Act
        patientService.addTag(clinicId, 1L, "diabetic");

        // Assert
        verify(patientTagRepository, never()).save(any());
    }
}

