package com.clinicos.emr.service;

import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.exception.ResourceNotFoundException;
import com.clinicos.emr.dto.DentalPlanDto;
import com.clinicos.emr.dto.DentalPriceListDto;
import com.clinicos.emr.entity.DentalPriceList;
import com.clinicos.emr.entity.DentalTreatmentPlan;
import com.clinicos.emr.repository.DentalPriceListRepository;
import com.clinicos.emr.repository.DentalTreatmentPlanRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Dental treatment plans and the clinic's procedure price list.
 * <p>
 * A plan's procedures ("items") are JSON so the screen can evolve without schema changes. On every
 * save the server checks the JSON and works out the plan total, the amount already billed and the
 * earliest lab work due, so clinic-wide lists don't have to read every plan's JSON.
 * <p>
 * Plan shape (fields the server reads): { items: [ { procedure, cost, status, billedAmount,
 * lab: { sentOn, dueOn, receivedOn } } ] }. Dates are YYYY-MM-DD.
 */
@Service
@RequiredArgsConstructor
public class DentalPlanService {

    static final int MAX_BYTES = 64 * 1024;
    static final int MAX_ITEMS = 100;
    static final int MAX_PROCEDURES = 200;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000");

    private static final ObjectMapper JSON = new ObjectMapper();

    private final DentalTreatmentPlanRepository plans;
    private final DentalPriceListRepository priceLists;

    // ---------------------------------------------------------------- plans

    @Transactional(readOnly = true)
    public List<DentalPlanDto> listForPatient(String clinicId, Long patientId) {
        return plans.findTop50ByClinicIdAndPatientIdOrderByCreatedAtDesc(clinicId, patientId)
                .stream().map(DentalPlanService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DentalPlanDto get(String clinicId, Long id) {
        return plans.findByIdAndClinicId(id, clinicId).map(DentalPlanService::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment plan not found: " + id));
    }

    /** Plans that still need attention: not finished, not fully billed, or with lab work pending. */
    @Transactional(readOnly = true)
    public List<DentalPlanDto> listOpen(String clinicId) {
        return plans.findTop300ByClinicIdAndStatusNotOrderByUpdatedAtDesc(clinicId, DentalTreatmentPlan.Status.CANCELLED)
                .stream()
                .filter(DentalPlanService::needsAttention)
                .map(DentalPlanService::toDto)
                .toList();
    }

    @Transactional
    public DentalPlanDto create(String clinicId, String user, DentalPlanDto input) {
        if (input == null || input.getPatientId() == null) throw bad("Choose a patient.");
        String by = who(user);
        DentalTreatmentPlan plan = DentalTreatmentPlan.builder()
                .clinicId(clinicId)
                .patientId(input.getPatientId())
                .patientName(limit(input.getPatientName(), 200))
                .title(limit(input.getTitle(), 200))
                .status(input.getStatus() == null ? DentalTreatmentPlan.Status.DRAFT : input.getStatus())
                .data("{}")
                .createdBy(by)
                .updatedBy(by)
                .build();
        apply(plan, input.getData());
        return toDto(plans.save(plan));
    }

    @Transactional
    public DentalPlanDto update(String clinicId, String user, Long id, DentalPlanDto input) {
        if (input == null) throw bad("Nothing to save.");
        DentalTreatmentPlan plan = plans.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment plan not found: " + id));
        if (input.getPatientId() != null && !input.getPatientId().equals(plan.getPatientId())) {
            throw bad("A plan can't be moved to another patient.");
        }
        if (input.getTitle() != null) plan.setTitle(limit(input.getTitle(), 200));
        if (input.getPatientName() != null) plan.setPatientName(limit(input.getPatientName(), 200));
        if (input.getStatus() != null) plan.setStatus(input.getStatus());
        if (input.getData() != null) apply(plan, input.getData());
        plan.setUpdatedBy(who(user));
        plan.setUpdatedAt(LocalDateTime.now());
        return toDto(plans.save(plan));
    }

    /** Checks the plan JSON and copies the derived totals and next lab due date onto the plan. */
    static void apply(DentalTreatmentPlan plan, JsonNode data) {
        if (data == null || !data.isObject()) throw bad("The plan is empty.");
        JsonNode items = data.path("items");
        if (!items.isMissingNode() && !items.isNull() && !items.isArray()) throw bad("The plan's procedures must be a list.");
        if (items.size() > MAX_ITEMS) throw bad("A plan can have at most " + MAX_ITEMS + " procedures.");

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal billed = BigDecimal.ZERO;
        LocalDate nextLab = null;
        for (JsonNode item : items) {
            if (!item.isObject()) throw bad("Each procedure must be an object.");
            if (text(item, "procedure").isEmpty()) throw bad("Every line in the plan needs a procedure.");
            boolean cancelled = "CANCELLED".equals(text(item, "status"));
            BigDecimal cost = money(item.path("cost"), "cost");
            if (!cancelled) total = total.add(cost);
            billed = billed.add(money(item.path("billedAmount"), "billed amount"));

            JsonNode lab = item.path("lab");
            if (!cancelled && lab.isObject()) {
                if (!text(lab, "sentOn").isEmpty()) date(text(lab, "sentOn"));
                if (!text(lab, "receivedOn").isEmpty()) date(text(lab, "receivedOn"));
                if (!text(lab, "dueOn").isEmpty()) {
                    LocalDate due = date(text(lab, "dueOn"));
                    boolean pending = !text(lab, "sentOn").isEmpty() && text(lab, "receivedOn").isEmpty();
                    if (pending && (nextLab == null || due.isBefore(nextLab))) nextLab = due;
                }
            }
        }

        String text = data.toString();
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw bad("The plan is too large. Shorten the notes.");
        plan.setData(text);
        plan.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        plan.setBilledAmount(billed.setScale(2, RoundingMode.HALF_UP));
        plan.setNextLabDue(nextLab);
    }

    static boolean needsAttention(DentalTreatmentPlan p) {
        if (p.getStatus() == DentalTreatmentPlan.Status.CANCELLED) return false;
        if (p.getStatus() != DentalTreatmentPlan.Status.COMPLETED) return true;
        return p.getNextLabDue() != null || p.getBilledAmount().compareTo(p.getTotalAmount()) < 0;
    }

    static DentalPlanDto toDto(DentalTreatmentPlan p) {
        return DentalPlanDto.builder()
                .id(p.getId()).patientId(p.getPatientId()).patientName(p.getPatientName()).title(p.getTitle())
                .status(p.getStatus()).totalAmount(p.getTotalAmount()).billedAmount(p.getBilledAmount())
                .nextLabDue(p.getNextLabDue()).data(read(p.getData()))
                .createdBy(p.getCreatedBy()).updatedBy(p.getUpdatedBy())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .build();
    }

    // ----------------------------------------------------------- price list

    @Transactional(readOnly = true)
    public DentalPriceListDto getPriceList(String clinicId) {
        return priceLists.findById(clinicId)
                .map(p -> DentalPriceListDto.builder().data(read(p.getData())).updatedBy(p.getUpdatedBy()).updatedAt(p.getUpdatedAt()).build())
                .orElseGet(() -> DentalPriceListDto.builder().build());
    }

    @Transactional
    public DentalPriceListDto savePriceList(String clinicId, String user, DentalPriceListDto input) {
        JsonNode data = input == null ? null : input.getData();
        if (data == null || !data.isObject() || !data.path("procedures").isArray()) throw bad("The price list is empty.");
        JsonNode procedures = data.path("procedures");
        if (procedures.size() > MAX_PROCEDURES) throw bad("A price list can have at most " + MAX_PROCEDURES + " procedures.");
        for (JsonNode p : procedures) {
            if (!p.isObject() || text(p, "name").isEmpty()) throw bad("Every procedure in the price list needs a name.");
            money(p.path("price"), "price");
        }
        String text = data.toString();
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw bad("The price list is too large.");

        DentalPriceList row = priceLists.findById(clinicId)
                .orElseGet(() -> DentalPriceList.builder().clinicId(clinicId).build());
        row.setData(text);
        row.setUpdatedBy(who(user));
        row.setUpdatedAt(LocalDateTime.now());
        DentalPriceList saved = priceLists.save(row);
        return DentalPriceListDto.builder().data(read(saved.getData())).updatedBy(saved.getUpdatedBy()).updatedAt(saved.getUpdatedAt()).build();
    }

    // -------------------------------------------------------------- helpers

    static BigDecimal money(JsonNode node, String what) {
        if (node == null || node.isMissingNode() || node.isNull()) return BigDecimal.ZERO;
        BigDecimal value;
        if (node.isNumber()) {
            value = node.decimalValue();
        } else if (node.isTextual()) {
            String s = node.asText().trim();
            if (s.isEmpty()) return BigDecimal.ZERO;
            try {
                value = new BigDecimal(s);
            } catch (NumberFormatException e) {
                throw bad("Enter a valid " + what + ".");
            }
        } else {
            throw bad("Enter a valid " + what + ".");
        }
        if (value.signum() < 0) throw bad("The " + what + " can't be negative.");
        if (value.compareTo(MAX_AMOUNT) > 0) throw bad("The " + what + " is too large.");
        return value;
    }

    static LocalDate date(String s) {
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw bad("Lab dates must be valid dates (YYYY-MM-DD).");
        }
    }

    static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isTextual() ? v.asText().trim() : "";
    }

    private static JsonNode read(String text) {
        try {
            return JSON.readTree(text == null ? "{}" : text);
        } catch (JsonProcessingException e) {
            return JSON.createObjectNode();
        }
    }

    private static String limit(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static String who(String user) {
        if (user == null || user.isBlank()) throw new RegistrationException(HttpStatus.UNAUTHORIZED, "Please sign in again.");
        return user.trim().toLowerCase(Locale.ROOT);
    }

    private static RegistrationException bad(String message) {
        return new RegistrationException(HttpStatus.BAD_REQUEST, message);
    }
}
