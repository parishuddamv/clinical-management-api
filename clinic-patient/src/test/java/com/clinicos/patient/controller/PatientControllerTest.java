package com.clinicos.patient.controller;

import com.clinicos.common.dto.ApiResponse;
import com.clinicos.patient.dto.*;
import com.clinicos.patient.entity.Patient;
import com.clinicos.patient.service.PatientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PatientController
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
@DisplayName("PatientController Integration Tests")
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatientService patientService;

    private String clinicId = "CLINIC_001";
    private PatientResponse patientResponse;
    private PatientSummaryResponse patientSummaryResponse;

    @BeforeEach
    void setUp() {
        patientResponse = PatientResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .phone("9876543210")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .gender(Patient.Gender.MALE)
                .bloodGroup("O+")
                .address("123 Main St")
                .emergencyContactName("Jane Doe")
                .emergencyContactPhone("9876543211")
                .isActive(true)
                .tags(List.of("diabetic"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        patientSummaryResponse = PatientSummaryResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .phone("9876543210")
                .gender("MALE")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("should register patient and return 201 CREATED")
    void testRegisterPatient() throws Exception {
        // Arrange
        RegisterPatientRequest request = RegisterPatientRequest.builder()
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

        when(patientService.registerPatient(nullable(String.class), any(RegisterPatientRequest.class)))
                .thenReturn(patientResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.firstName", is("John")));
    }

    @Test
    @DisplayName("should get patient by ID and return 200 OK")
    void testGetPatientById() throws Exception {
        // Arrange
        when(patientService.getPatientById(nullable(String.class), eq(1L)))
                .thenReturn(patientResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.phone", is("9876543210")));
    }

    @Test
    @DisplayName("should update patient and return 200 OK")
    void testUpdatePatient() throws Exception {
        // Arrange
        UpdatePatientRequest updateRequest = UpdatePatientRequest.builder()
                .firstName("Johnny")
                .build();

        when(patientService.updatePatient(nullable(String.class), eq(1L), any(UpdatePatientRequest.class)))
                .thenReturn(patientResponse);

        // Act & Assert
        mockMvc.perform(put("/api/v1/patients/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("should delete patient and return 200 OK")
    void testDeletePatient() throws Exception {
        // Arrange
        doNothing().when(patientService).softDeletePatient(nullable(String.class), eq(1L));

        // Act & Assert
        mockMvc.perform(delete("/api/v1/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("should search patients and return paginated results")
    void testSearchPatients() throws Exception {
        // Arrange
        Page<PatientSummaryResponse> page = new PageImpl<>(
                List.of(patientSummaryResponse),
                PageRequest.of(0, 20),
                1
        );

        when(patientService.searchPatients(nullable(String.class), anyString(), any()))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/patients/search")
                .param("q", "John")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("should add tag to patient and return 201 CREATED")
    void testAddTag() throws Exception {
        // Arrange
        PatientController.TagRequest tagRequest = new PatientController.TagRequest("diabetic");
        doNothing().when(patientService).addTag(nullable(String.class), eq(1L), anyString());

        // Act & Assert
        mockMvc.perform(post("/api/v1/patients/1/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tagRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("should remove tag from patient and return 200 OK")
    void testRemoveTag() throws Exception {
        // Arrange
        doNothing().when(patientService).removeTag(nullable(String.class), eq(1L), eq("diabetic"));

        // Act & Assert
        mockMvc.perform(delete("/api/v1/patients/1/tags/diabetic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}

