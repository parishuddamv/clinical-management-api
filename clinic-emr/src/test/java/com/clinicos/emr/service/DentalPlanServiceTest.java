package com.clinicos.emr.service;

import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.exception.ResourceNotFoundException;
import com.clinicos.emr.dto.DentalPlanDto;
import com.clinicos.emr.dto.DentalPriceListDto;
import com.clinicos.emr.entity.DentalPriceList;
import com.clinicos.emr.entity.DentalTreatmentPlan;
import com.clinicos.emr.repository.DentalPriceListRepository;
import com.clinicos.emr.repository.DentalTreatmentPlanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DentalPlanServiceTest {

    private final ObjectMapper json = new ObjectMapper();
    private final DentalTreatmentPlanRepository plans = mock(DentalTreatmentPlanRepository.class);
    private final DentalPriceListRepository priceLists = mock(DentalPriceListRepository.class);
    private final DentalPlanService service = new DentalPlanService(plans, priceLists);

    @BeforeEach
    void setup() {
        when(plans.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(priceLists.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ObjectNode item(String procedure, Object cost, String status) {
        ObjectNode i = json.createObjectNode().put("procedure", procedure).put("tooth", "36");
        if (cost instanceof Number n) i.put("cost", new BigDecimal(n.toString()));
        else if (cost != null) i.put("cost", cost.toString());
        if (status != null) i.put("status", status);
        return i;
    }

    private ObjectNode plan(ObjectNode... items) {
        ObjectNode data = json.createObjectNode();
        ArrayNode list = data.putArray("items");
        for (ObjectNode i : items) list.add(i);
        return data;
    }

    @Test
    void savesAPlanAndWorksOutTheTotalsAndNextLabDue() {
        ObjectNode rct = item("Root canal treatment", 4500, "IN_PROGRESS").put("billedAmount", 1500);
        ObjectNode crown = item("Crown – zirconia", "8000", "PLANNED");
        crown.putObject("lab").put("sentOn", "2026-11-01").put("dueOn", "2026-11-10");
        ObjectNode bridge = item("Bridge", 9000, "PLANNED");
        bridge.putObject("lab").put("sentOn", "2026-11-02").put("dueOn", "2026-11-08").put("receivedOn", "2026-11-05");
        ObjectNode cancelled = item("Extraction", 800, "CANCELLED");
        cancelled.putObject("lab").put("sentOn", "2026-11-01").put("dueOn", "2026-11-03");

        DentalPlanDto result = service.create("C1", " Dr.Asha@Clinic.in ", DentalPlanDto.builder()
                .patientId(9L).patientName(" Anny S ").data(plan(rct, crown, bridge, cancelled)).build());

        ArgumentCaptor<DentalTreatmentPlan> saved = ArgumentCaptor.forClass(DentalTreatmentPlan.class);
        verify(plans).save(saved.capture());
        DentalTreatmentPlan p = saved.getValue();
        assertThat(p.getClinicId()).isEqualTo("C1");
        assertThat(p.getCreatedBy()).isEqualTo("dr.asha@clinic.in");
        assertThat(p.getPatientName()).isEqualTo("Anny S");
        assertThat(p.getStatus()).isEqualTo(DentalTreatmentPlan.Status.DRAFT);
        assertThat(p.getTotalAmount()).isEqualByComparingTo("21500");   // cancelled extraction excluded
        assertThat(p.getBilledAmount()).isEqualByComparingTo("1500");
        assertThat(p.getNextLabDue()).isEqualTo(LocalDate.of(2026, 11, 10)); // received and cancelled lab work ignored
        assertThat(result.getData().path("items").size()).isEqualTo(4);
    }

    @Test
    void rejectsBadPlans() {
        assertThatThrownBy(() -> service.create("C1", "a@b.in", DentalPlanDto.builder().data(plan()).build()))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("patient");
        assertThatThrownBy(() -> service.create("C1", "a@b.in", DentalPlanDto.builder().patientId(9L).build()))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("empty");
        assertThatThrownBy(() -> service.create("C1", "a@b.in", DentalPlanDto.builder().patientId(9L).data(plan(item("", 100, null))).build()))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("procedure");
        assertThatThrownBy(() -> service.create("C1", "a@b.in", DentalPlanDto.builder().patientId(9L).data(plan(item("Filling", -5, null))).build()))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("negative");
        assertThatThrownBy(() -> service.create("C1", "a@b.in", DentalPlanDto.builder().patientId(9L).data(plan(item("Filling", "abc", null))).build()))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("valid cost");
        ObjectNode badLab = item("Crown", 100, null);
        badLab.putObject("lab").put("sentOn", "2026-11-01").put("dueOn", "next week");
        assertThatThrownBy(() -> service.create("C1", "a@b.in", DentalPlanDto.builder().patientId(9L).data(plan(badLab)).build()))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("YYYY-MM-DD");
    }

    @Test
    void updatesStayInTheClinicAndOnTheSamePatient() {
        DentalTreatmentPlan existing = DentalTreatmentPlan.builder().id(5L).clinicId("C1").patientId(9L)
                .status(DentalTreatmentPlan.Status.PROPOSED).data("{\"items\":[]}").createdBy("x").updatedBy("x").build();
        when(plans.findByIdAndClinicId(5L, "C1")).thenReturn(Optional.of(existing));
        when(plans.findByIdAndClinicId(5L, "C2")).thenReturn(Optional.empty());

        DentalPlanDto updated = service.update("C1", "b@c.in", 5L, DentalPlanDto.builder()
                .status(DentalTreatmentPlan.Status.ACCEPTED).data(plan(item("Scaling", 1200, null))).build());
        assertThat(updated.getStatus()).isEqualTo(DentalTreatmentPlan.Status.ACCEPTED);
        assertThat(updated.getTotalAmount()).isEqualByComparingTo("1200");
        assertThat(updated.getUpdatedBy()).isEqualTo("b@c.in");

        assertThatThrownBy(() -> service.update("C2", "b@c.in", 5L, DentalPlanDto.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.update("C1", "b@c.in", 5L, DentalPlanDto.builder().patientId(10L).build()))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("another patient");
    }

    @Test
    void openListSkipsFinishedFullyBilledPlans() {
        DentalTreatmentPlan active = DentalTreatmentPlan.builder().status(DentalTreatmentPlan.Status.IN_PROGRESS)
                .totalAmount(new BigDecimal("100")).billedAmount(BigDecimal.ZERO).data("{}").build();
        DentalTreatmentPlan doneUnbilled = DentalTreatmentPlan.builder().status(DentalTreatmentPlan.Status.COMPLETED)
                .totalAmount(new BigDecimal("100")).billedAmount(new BigDecimal("40")).data("{}").build();
        DentalTreatmentPlan doneLabPending = DentalTreatmentPlan.builder().status(DentalTreatmentPlan.Status.COMPLETED)
                .totalAmount(new BigDecimal("100")).billedAmount(new BigDecimal("100")).nextLabDue(LocalDate.now()).data("{}").build();
        DentalTreatmentPlan finished = DentalTreatmentPlan.builder().status(DentalTreatmentPlan.Status.COMPLETED)
                .totalAmount(new BigDecimal("100")).billedAmount(new BigDecimal("100")).data("{}").build();
        when(plans.findTop300ByClinicIdAndStatusNotOrderByUpdatedAtDesc("C1", DentalTreatmentPlan.Status.CANCELLED))
                .thenReturn(List.of(active, doneUnbilled, doneLabPending, finished));

        assertThat(service.listOpen("C1")).hasSize(3);
    }

    @Test
    void priceListIsSavedPerClinic() {
        when(priceLists.findById("C1")).thenReturn(Optional.empty());
        ObjectNode data = json.createObjectNode();
        data.putArray("procedures").addObject().put("name", "Scaling").put("price", 1200);

        DentalPriceListDto saved = service.savePriceList("C1", "a@b.in", DentalPriceListDto.builder().data(data).build());
        ArgumentCaptor<DentalPriceList> row = ArgumentCaptor.forClass(DentalPriceList.class);
        verify(priceLists).save(row.capture());
        assertThat(row.getValue().getClinicId()).isEqualTo("C1");
        assertThat(saved.getData().path("procedures").get(0).path("name").asText()).isEqualTo("Scaling");

        ObjectNode bad = json.createObjectNode();
        bad.putArray("procedures").addObject().put("name", " ").put("price", 10);
        assertThatThrownBy(() -> service.savePriceList("C1", "a@b.in", DentalPriceListDto.builder().data(bad).build()))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("name");
        assertThat(service.getPriceList("C9").getData()).isNull();
    }
}
