package com.clinicos.emr.service;

import com.clinicos.common.exception.ResourceNotFoundException;
import com.clinicos.emr.dto.FavouriteDrugDto;
import com.clinicos.emr.entity.FavouriteDrug;
import com.clinicos.emr.repository.FavouriteDrugRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FavouriteDrugServiceTest {

    private final FavouriteDrugRepository repo = mock(FavouriteDrugRepository.class);
    private final FavouriteDrugService service = new FavouriteDrugService(repo);

    @BeforeEach
    void setup() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void newFavouriteBelongsToTheDoctorInTheirClinic() {
        when(repo.findSame("C1", "neha@clinic.in", "Paracetamol", "500 mg")).thenReturn(Optional.empty());

        service.save("C1", " Neha@Clinic.in ", FavouriteDrugDto.builder().drugName(" Paracetamol ").strength("500 mg").frequency("TDS").duration("3 days").build());

        ArgumentCaptor<FavouriteDrug> saved = ArgumentCaptor.forClass(FavouriteDrug.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getClinicId()).isEqualTo("C1");
        assertThat(saved.getValue().getDoctorUser()).isEqualTo("neha@clinic.in");
        assertThat(saved.getValue().getDrugName()).isEqualTo("Paracetamol");
        assertThat(saved.getValue().getFrequency()).isEqualTo("TDS");
    }

    @Test
    void savingTheSameMedicineUpdatesItsDoseTemplate() {
        FavouriteDrug existing = FavouriteDrug.builder().id(4L).clinicId("C1").doctorUser("neha@clinic.in").drugName("Paracetamol").strength("500 mg").frequency("BD").useCount(7).build();
        when(repo.findSame(anyString(), anyString(), anyString(), any())).thenReturn(Optional.of(existing));

        FavouriteDrugDto result = service.save("C1", "neha@clinic.in", FavouriteDrugDto.builder().drugName("paracetamol").strength("500 mg").frequency("TDS").build());

        assertThat(result.getId()).isEqualTo(4L);
        assertThat(result.getFrequency()).isEqualTo("TDS");
        assertThat(result.getUseCount()).isEqualTo(7);
        verify(repo, never()).countByClinicIdAndDoctorUser(any(), any());
    }

    @Test
    void doctorsCannotTouchAnotherDoctorsFavourites() {
        when(repo.findByIdAndClinicIdAndDoctorUser(4L, "C1", "ravi@clinic.in")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete("C1", "ravi@clinic.in", 4L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void limitPerDoctor() {
        when(repo.findSame(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(repo.countByClinicIdAndDoctorUser("C1", "neha@clinic.in")).thenReturn((long) FavouriteDrugService.MAX_PER_DOCTOR);
        assertThatThrownBy(() -> service.save("C1", "neha@clinic.in", FavouriteDrugDto.builder().drugName("X").build())).hasMessageContaining("up to");
    }
}
