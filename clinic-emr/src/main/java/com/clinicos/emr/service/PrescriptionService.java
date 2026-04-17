package com.clinicos.emr.service;

import com.clinicos.emr.dto.*;
import com.clinicos.emr.dto.CreatePrescriptionRequest.PrescriptionItemRequest;
import com.clinicos.emr.dto.PrescriptionResponse.PrescriptionItemResponse;
import com.clinicos.emr.entity.*;
import com.clinicos.emr.entity.Prescription.DeliveryMethod;
import com.clinicos.emr.entity.Prescription.PrescriptionStatus;
import com.clinicos.emr.repository.*;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PatientVisitRepository visitRepository;
    private final DrugMasterRepository drugRepository;
    private final PrescriptionPdfService pdfService;

    @Transactional
    public PrescriptionResponse createPrescription(String clinicId, CreatePrescriptionRequest request) {
        log.info("Creating prescription for patient: {} in clinic: {}", request.getPatientId(), clinicId);

        String prescriptionNumber = generatePrescriptionNumber(clinicId);

        Prescription prescription = Prescription.builder()
                .prescriptionNumber(prescriptionNumber)
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .doctorName(request.getDoctorName())
                .doctorLicenseNo(request.getDoctorLicenseNo())
                .doctorSpecialization(request.getDoctorSpecialization())
                .prescriptionDate(request.getPrescriptionDate() != null ? request.getPrescriptionDate() : LocalDate.now())
                .validUntil(request.getValidUntil())
                .diagnosisSummary(request.getDiagnosisSummary())
                .specialInstructions(request.getSpecialInstructions())
                .status(PrescriptionStatus.DRAFT)
                .notes(request.getNotes())
                .build();

        prescription.setClinicId(clinicId);

        // Link to visit if provided
        if (request.getVisitId() != null) {
            PatientVisit visit = visitRepository.findByIdAndClinicId(request.getVisitId(), clinicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + request.getVisitId()));
            prescription.setVisit(visit);
        }

        // Add prescription items
        int order = 0;
        for (PrescriptionItemRequest itemReq : request.getItems()) {
            PrescriptionItem item = PrescriptionItem.builder()
                    .drugName(itemReq.getDrugName())
                    .genericName(itemReq.getGenericName())
                    .strength(itemReq.getStrength())
                    .form(itemReq.getForm())
                    .dosage(itemReq.getDosage())
                    .frequency(itemReq.getFrequency())
                    .duration(itemReq.getDuration())
                    .quantity(itemReq.getQuantity())
                    .route(itemReq.getRoute())
                    .timing(itemReq.getTiming())
                    .beforeAfterFood(itemReq.getBeforeAfterFood())
                    .specialInstructions(itemReq.getSpecialInstructions())
                    .sequenceOrder(itemReq.getSequenceOrder() != null ? itemReq.getSequenceOrder() : order++)
                    .build();

            if (itemReq.getDrugId() != null) {
                drugRepository.findById(itemReq.getDrugId()).ifPresent(item::setDrug);
            }

            prescription.addItem(item);
        }

        prescription = prescriptionRepository.save(prescription);
        log.info("Created prescription: {} with {} items", prescriptionNumber, prescription.getItems().size());

        return mapToResponse(prescription);
    }

    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescription(String clinicId, Long id) {
        Prescription prescription = prescriptionRepository.findByIdWithItemsAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));
        return mapToResponse(prescription);
    }

    @Transactional
    public PrescriptionResponse finalizePrescription(String clinicId, Long id) {
        Prescription prescription = prescriptionRepository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));

        if (prescription.getStatus() != PrescriptionStatus.DRAFT) {
            throw new IllegalStateException("Only draft prescriptions can be finalized");
        }

        prescription.setStatus(PrescriptionStatus.FINALIZED);
        prescription = prescriptionRepository.save(prescription);

        log.info("Finalized prescription: {}", prescription.getPrescriptionNumber());
        return mapToResponse(prescription);
    }

    @Transactional
    public PrescriptionResponse deliverPrescription(String clinicId, Long id, DeliverPrescriptionRequest request) {
        Prescription prescription = prescriptionRepository.findByIdWithItemsAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));

        if (prescription.getStatus() == PrescriptionStatus.DRAFT) {
            throw new IllegalStateException("Prescription must be finalized before delivery");
        }

        // Generate PDF
        String pdfPath = pdfService.generatePrescriptionPdf(prescription, request);
        prescription.setPdfFilePath(pdfPath);

        // Set delivery info
        prescription.setDeliveryMethod(request.getDeliveryMethod());
        prescription.setDeliveredAt(LocalDateTime.now());

        // Handle delivery based on method
        String deliveryRef = handleDelivery(prescription, request);
        prescription.setDeliveryReference(deliveryRef);
        prescription.setStatus(PrescriptionStatus.DELIVERED);

        prescription = prescriptionRepository.save(prescription);
        log.info("Delivered prescription: {} via {}", prescription.getPrescriptionNumber(), request.getDeliveryMethod());

        return mapToResponse(prescription);
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponse> getPatientPrescriptions(String clinicId, Long patientId, Pageable pageable) {
        return prescriptionRepository.findByClinicIdAndPatientIdOrderByPrescriptionDateDesc(clinicId, patientId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponse> getAllPrescriptions(String clinicId, PrescriptionStatus status, Pageable pageable) {
        if (status != null) {
            return prescriptionRepository.findByClinicIdAndStatusOrderByCreatedAtDesc(clinicId, status, pageable)
                    .map(this::mapToResponse);
        }
        return prescriptionRepository.findByClinicIdOrderByCreatedAtDesc(clinicId, pageable)
                .map(this::mapToResponse);
    }

    // Drug auto-suggest
    @Transactional(readOnly = true)
    public List<DrugResponse> searchDrugs(String clinicId, String query) {
        return drugRepository.searchDrugs(clinicId, query, PageRequest.of(0, 20))
                .stream()
                .map(this::mapDrugToResponse)
                .collect(Collectors.toList());
    }

    private String generatePrescriptionNumber(String clinicId) {
        String prefix = "RX" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long maxNum = prescriptionRepository.findMaxPrescriptionNumber(clinicId, prefix);
        long nextNum = (maxNum != null ? maxNum : 0) + 1;
        return prefix + String.format("%04d", nextNum);
    }

    private String handleDelivery(Prescription prescription, DeliverPrescriptionRequest request) {
        switch (request.getDeliveryMethod()) {
            case PRINT:
                return "PRINT_" + System.currentTimeMillis();
            case EMAIL:
                // TODO: Call notification service to send email with PDF attachment
                log.info("Sending prescription via email to: {}", request.getRecipientEmail());
                return "EMAIL_" + request.getRecipientEmail();
            case WHATSAPP:
                // Generate WhatsApp deep link
                String message = buildWhatsAppMessage(prescription, request);
                String whatsappLink = "https://wa.me/" + request.getRecipientPhone() + "?text=" + encodeUrl(message);
                log.info("WhatsApp link generated for: {}", request.getRecipientPhone());
                return whatsappLink;
            case SMS:
                // TODO: Call notification service to send SMS
                log.info("Sending prescription via SMS to: {}", request.getRecipientPhone());
                return "SMS_" + request.getRecipientPhone();
            default:
                return "UNKNOWN";
        }
    }

    private String buildWhatsAppMessage(Prescription prescription, DeliverPrescriptionRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Prescription from ").append(request.getClinicName()).append("*\n\n");
        sb.append("Patient: ").append(request.getPatientName()).append("\n");
        sb.append("Date: ").append(prescription.getPrescriptionDate()).append("\n");
        sb.append("Rx No: ").append(prescription.getPrescriptionNumber()).append("\n\n");
        sb.append("*Medications:*\n");
        
        for (PrescriptionItem item : prescription.getItems()) {
            sb.append("• ").append(item.getDrugName());
            if (item.getStrength() != null) sb.append(" ").append(item.getStrength());
            sb.append("\n  ").append(item.getDosage()).append(" - ").append(item.getFrequency());
            sb.append(" for ").append(item.getDuration()).append("\n");
        }
        
        if (prescription.getSpecialInstructions() != null) {
            sb.append("\n*Instructions:* ").append(prescription.getSpecialInstructions());
        }
        
        sb.append("\n\n").append(request.getClinicName());
        if (request.getClinicPhone() != null) {
            sb.append("\nPh: ").append(request.getClinicPhone());
        }
        
        return sb.toString();
    }

    private String encodeUrl(String text) {
        try {
            return java.net.URLEncoder.encode(text, "UTF-8");
        } catch (Exception e) {
            return text;
        }
    }

    private PrescriptionResponse mapToResponse(Prescription p) {
        return PrescriptionResponse.builder()
                .id(p.getId())
                .prescriptionNumber(p.getPrescriptionNumber())
                .visitId(p.getVisit() != null ? p.getVisit().getId() : null)
                .patientId(p.getPatientId())
                .doctorId(p.getDoctorId())
                .doctorName(p.getDoctorName())
                .doctorLicenseNo(p.getDoctorLicenseNo())
                .doctorSpecialization(p.getDoctorSpecialization())
                .prescriptionDate(p.getPrescriptionDate())
                .validUntil(p.getValidUntil())
                .diagnosisSummary(p.getDiagnosisSummary())
                .specialInstructions(p.getSpecialInstructions())
                .status(p.getStatus())
                .deliveryMethod(p.getDeliveryMethod())
                .deliveredAt(p.getDeliveredAt())
                .deliveryReference(p.getDeliveryReference())
                .pdfFilePath(p.getPdfFilePath())
                .notes(p.getNotes())
                .items(p.getItems().stream().map(this::mapItemToResponse).collect(Collectors.toList()))
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private PrescriptionItemResponse mapItemToResponse(PrescriptionItem item) {
        return PrescriptionItemResponse.builder()
                .id(item.getId())
                .drugId(item.getDrug() != null ? item.getDrug().getId() : null)
                .drugName(item.getDrugName())
                .genericName(item.getGenericName())
                .strength(item.getStrength())
                .form(item.getForm())
                .dosage(item.getDosage())
                .frequency(item.getFrequency())
                .duration(item.getDuration())
                .quantity(item.getQuantity())
                .route(item.getRoute())
                .timing(item.getTiming())
                .beforeAfterFood(item.getBeforeAfterFood())
                .specialInstructions(item.getSpecialInstructions())
                .sequenceOrder(item.getSequenceOrder())
                .build();
    }

    private DrugResponse mapDrugToResponse(DrugMaster drug) {
        return DrugResponse.builder()
                .id(drug.getId())
                .drugCode(drug.getDrugCode())
                .brandName(drug.getBrandName())
                .genericName(drug.getGenericName())
                .strength(drug.getStrength())
                .form(drug.getForm())
                .manufacturer(drug.getManufacturer())
                .category(drug.getCategory())
                .scheduleType(drug.getScheduleType())
                .unitPrice(drug.getUnitPrice())
                .build();
    }
}

