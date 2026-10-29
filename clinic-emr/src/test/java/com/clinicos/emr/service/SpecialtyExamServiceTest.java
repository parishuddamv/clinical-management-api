package com.clinicos.emr.service;

import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.exception.ResourceNotFoundException;
import com.clinicos.emr.dto.SpecialtyExamDto;
import com.clinicos.emr.entity.SpecialtyExam;
import com.clinicos.emr.repository.SpecialtyExamRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SpecialtyExamServiceTest {

    private final ObjectMapper json = new ObjectMapper();
    private final SpecialtyExamRepository repo = mock(SpecialtyExamRepository.class);
    private final SpecialtyExamService service = new SpecialtyExamService(repo);

    @BeforeEach
    void setup() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ObjectNode dental() {
        ObjectNode data = json.createObjectNode();
        data.putObject("teeth").putObject("16").put("status", "CARIES");
        return data;
    }

    @Test
    void savesTheChartForThePatientInTheClinic() {
        SpecialtyExamDto result = service.create("C1", " Neha@Clinic.in ", SpecialtyExamDto.builder().patientId(9L).visitId(3L).examType(SpecialtyExam.ExamType.DENTAL).data(dental()).build());

        ArgumentCaptor<SpecialtyExam> saved = ArgumentCaptor.forClass(SpecialtyExam.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getClinicId()).isEqualTo("C1");
        assertThat(saved.getValue().getRecordedBy()).isEqualTo("neha@clinic.in");
        assertThat(saved.getValue().getData()).isEqualTo("{\"teeth\":{\"16\":{\"status\":\"CARIES\"}}}");
        assertThat(result.getData().path("teeth").path("16").path("status").asText()).isEqualTo("CARIES");
    }

    @Test
    void rejectsEmptyOrOversizedCharts() {
        assertThatThrownBy(() -> service.create("C1", "a@b.in", SpecialtyExamDto.builder().patientId(9L).examType(SpecialtyExam.ExamType.EYE).build()))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("empty");
        ObjectNode big = json.createObjectNode().put("notes", "x".repeat(SpecialtyExamService.MAX_BYTES));
        assertThatThrownBy(() -> service.create("C1", "a@b.in", SpecialtyExamDto.builder().patientId(9L).examType(SpecialtyExam.ExamType.EYE).data(big).build()))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("too large");
        assertThatThrownBy(() -> service.create("C1", "a@b.in", SpecialtyExamDto.builder().patientId(9L).data(dental()).build()))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("chart type");
    }

    @Test
    void chartsCanBeCorrectedForADayOnly() {
        SpecialtyExam recent = SpecialtyExam.builder().id(1L).clinicId("C1").patientId(9L).examType(SpecialtyExam.ExamType.DENTAL).data("{}").recordedBy("x").createdAt(LocalDateTime.now().minusHours(2)).build();
        SpecialtyExam old = SpecialtyExam.builder().id(2L).clinicId("C1").patientId(9L).examType(SpecialtyExam.ExamType.DENTAL).data("{}").recordedBy("x").createdAt(LocalDateTime.now().minusDays(2)).build();
        when(repo.findByIdAndClinicId(1L, "C1")).thenReturn(Optional.of(recent));
        when(repo.findByIdAndClinicId(2L, "C1")).thenReturn(Optional.of(old));

        assertThat(service.update("C1", "a@b.in", 1L, SpecialtyExamDto.builder().data(dental()).build()).getData().has("teeth")).isTrue();
        assertThatThrownBy(() -> service.update("C1", "a@b.in", 2L, SpecialtyExamDto.builder().data(dental()).build()))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("older than a day");
        assertThatThrownBy(() -> service.update("C1", "a@b.in", 1L, SpecialtyExamDto.builder().examType(SpecialtyExam.ExamType.SKIN).data(dental()).build()))
                .isInstanceOf(RegistrationException.class);
    }

    @Test
    void anotherClinicsChartIsNotFound() {
        when(repo.findByIdAndClinicId(1L, "C2")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update("C2", "a@b.in", 1L, SpecialtyExamDto.builder().data(dental()).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
