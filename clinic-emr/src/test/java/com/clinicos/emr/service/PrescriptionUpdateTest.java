package com.clinicos.emr.service;

import com.clinicos.common.exception.RegistrationException;
import com.clinicos.emr.dto.CreatePrescriptionRequest;
import com.clinicos.emr.entity.Prescription;
import com.clinicos.emr.entity.PrescriptionItem;
import com.clinicos.emr.repository.DrugMasterRepository;
import com.clinicos.emr.repository.PatientVisitRepository;
import com.clinicos.emr.repository.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PrescriptionUpdateTest {

    private final PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
    private PrescriptionService service;

    @BeforeEach
    void setup() {
        service = new PrescriptionService(prescriptions, mock(PatientVisitRepository.class), mock(DrugMasterRepository.class), mock(PrescriptionPdfService.class));
        when(prescriptions.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Prescription draft() {
        Prescription p = Prescription.builder().prescriptionNumber("RX-1").patientId(5L).status(Prescription.PrescriptionStatus.DRAFT).build();
        p.addItem(PrescriptionItem.builder().drugName("Old drug").dosage("1").frequency("OD").duration("3 days").build());
        return p;
    }

    private CreatePrescriptionRequest request(Long patientId) {
        CreatePrescriptionRequest.PrescriptionItemRequest item = new CreatePrescriptionRequest.PrescriptionItemRequest();
        item.setDrugName("Paracetamol");
        item.setDosage("500 mg");
        item.setFrequency("TDS");
        item.setDuration("5 days");
        CreatePrescriptionRequest r = new CreatePrescriptionRequest();
        r.setPatientId(patientId);
        r.setDiagnosisSummary("Fever");
        r.setItems(List.of(item));
        return r;
    }

    @Test
    void draftIsUpdatedAndItemsReplaced() {
        Prescription p = draft();
        when(prescriptions.findByIdWithItemsAndClinicId(1L, "C1")).thenReturn(Optional.of(p));

        var response = service.updatePrescription("C1", 1L, request(5L));

        assertThat(response.getDiagnosisSummary()).isEqualTo("Fever");
        assertThat(p.getItems()).extracting(PrescriptionItem::getDrugName).containsExactly("Paracetamol");
    }

    @Test
    void finalisedPrescriptionCannotBeEdited() {
        Prescription p = draft();
        p.setStatus(Prescription.PrescriptionStatus.FINALIZED);
        when(prescriptions.findByIdWithItemsAndClinicId(1L, "C1")).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.updatePrescription("C1", 1L, request(5L)))
                .isInstanceOfSatisfying(RegistrationException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void prescriptionCannotMoveToAnotherPatient() {
        when(prescriptions.findByIdWithItemsAndClinicId(1L, "C1")).thenReturn(Optional.of(draft()));
        assertThatThrownBy(() -> service.updatePrescription("C1", 1L, request(6L))).hasMessageContaining("another patient");
    }

    @Test
    void markPrintedRecordsTheTime() {
        Prescription p = draft();
        when(prescriptions.findByIdWithItemsAndClinicId(1L, "C1")).thenReturn(Optional.of(p));
        assertThat(service.markPrinted("C1", 1L).getPrintedAt()).isNotNull();
    }
}
