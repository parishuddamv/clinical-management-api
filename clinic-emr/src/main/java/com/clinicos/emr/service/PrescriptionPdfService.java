package com.clinicos.emr.service;

import com.clinicos.emr.dto.DeliverPrescriptionRequest;
import com.clinicos.emr.entity.Prescription;
import com.clinicos.emr.entity.PrescriptionItem;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PrescriptionPdfService {

    @Value("${app.file-storage.local.base-path:./uploads}")
    private String uploadBasePath;

    public String generatePrescriptionPdf(Prescription prescription, DeliverPrescriptionRequest request) {
        try {
            // Create directory if not exists
            Path prescriptionDir = Paths.get(uploadBasePath, "prescriptions", prescription.getClinicId());
            Files.createDirectories(prescriptionDir);

            String fileName = prescription.getPrescriptionNumber() + ".pdf";
            String filePath = prescriptionDir.resolve(fileName).toString();

            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Header - Clinic Info
            addClinicHeader(document, request, boldFont, normalFont);

            // Prescription Info
            addPrescriptionInfo(document, prescription, request, boldFont, normalFont);

            // Medications Table
            addMedicationsTable(document, prescription, boldFont, normalFont);

            // Instructions
            if (prescription.getSpecialInstructions() != null) {
                document.add(new Paragraph("\nSpecial Instructions:")
                        .setFont(boldFont).setFontSize(11));
                document.add(new Paragraph(prescription.getSpecialInstructions())
                        .setFont(normalFont).setFontSize(10));
            }

            // Footer
            addFooter(document, prescription, boldFont, normalFont);

            document.close();
            log.info("Generated prescription PDF: {}", filePath);

            return filePath;
        } catch (IOException e) {
            log.error("Error generating prescription PDF", e);
            throw new RuntimeException("Failed to generate prescription PDF", e);
        }
    }

    private void addClinicHeader(Document document, DeliverPrescriptionRequest request, 
                                  PdfFont boldFont, PdfFont normalFont) {
        // Clinic Name
        Paragraph clinicName = new Paragraph(request.getClinicName() != null ? request.getClinicName() : "Medical Clinic")
                .setFont(boldFont)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY);
        document.add(clinicName);

        // Clinic Address
        if (request.getClinicAddress() != null) {
            document.add(new Paragraph(request.getClinicAddress())
                    .setFont(normalFont)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        // Clinic Phone
        if (request.getClinicPhone() != null) {
            document.add(new Paragraph("Phone: " + request.getClinicPhone())
                    .setFont(normalFont)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        // Line separator
        document.add(new Paragraph("─".repeat(80))
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY));

        // Rx Symbol
        document.add(new Paragraph("℞")
                .setFont(boldFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginTop(10));
    }

    private void addPrescriptionInfo(Document document, Prescription prescription, 
                                      DeliverPrescriptionRequest request, PdfFont boldFont, PdfFont normalFont) {
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(10);

        // Left column - Patient info
        Cell leftCell = new Cell().setBorder(Border.NO_BORDER);
        leftCell.add(new Paragraph("Patient: " + (request.getPatientName() != null ? request.getPatientName() : ""))
                .setFont(normalFont).setFontSize(11));
        
        // Right column - Prescription info
        Cell rightCell = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);
        rightCell.add(new Paragraph("Rx No: " + prescription.getPrescriptionNumber())
                .setFont(boldFont).setFontSize(11));
        rightCell.add(new Paragraph("Date: " + prescription.getPrescriptionDate()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                .setFont(normalFont).setFontSize(10));

        infoTable.addCell(leftCell);
        infoTable.addCell(rightCell);
        document.add(infoTable);

        // Diagnosis if available
        if (prescription.getDiagnosisSummary() != null) {
            document.add(new Paragraph("Diagnosis: " + prescription.getDiagnosisSummary())
                    .setFont(normalFont)
                    .setFontSize(10)
                    .setItalic()
                    .setMarginTop(5));
        }
    }

    private void addMedicationsTable(Document document, Prescription prescription, 
                                      PdfFont boldFont, PdfFont normalFont) {
        document.add(new Paragraph("\nMedications:")
                .setFont(boldFont)
                .setFontSize(12)
                .setMarginTop(15));

        Table table = new Table(UnitValue.createPercentArray(new float[]{0.5f, 3, 2, 2, 2, 2}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(5);

        // Header row
        String[] headers = {"#", "Medication", "Dosage", "Frequency", "Duration", "Instructions"};
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header).setFont(boldFont).setFontSize(9))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        // Data rows
        int index = 1;
        for (PrescriptionItem item : prescription.getItems()) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(index++))
                    .setFont(normalFont).setFontSize(9)).setTextAlignment(TextAlignment.CENTER));

            // Medication name with strength
            String medName = item.getDrugName();
            if (item.getStrength() != null) medName += " " + item.getStrength();
            if (item.getForm() != null) medName += " (" + item.getForm() + ")";
            table.addCell(new Cell().add(new Paragraph(medName)
                    .setFont(normalFont).setFontSize(9)));

            table.addCell(new Cell().add(new Paragraph(item.getDosage())
                    .setFont(normalFont).setFontSize(9)).setTextAlignment(TextAlignment.CENTER));

            table.addCell(new Cell().add(new Paragraph(item.getFrequency())
                    .setFont(normalFont).setFontSize(9)).setTextAlignment(TextAlignment.CENTER));

            table.addCell(new Cell().add(new Paragraph(item.getDuration())
                    .setFont(normalFont).setFontSize(9)).setTextAlignment(TextAlignment.CENTER));

            String instructions = "";
            if (item.getBeforeAfterFood() != null) {
                instructions = item.getBeforeAfterFood().name().replace("_", " ");
            }
            if (item.getSpecialInstructions() != null) {
                instructions += (instructions.isEmpty() ? "" : ", ") + item.getSpecialInstructions();
            }
            table.addCell(new Cell().add(new Paragraph(instructions)
                    .setFont(normalFont).setFontSize(8)));
        }

        document.add(table);
    }

    private void addFooter(Document document, Prescription prescription, 
                           PdfFont boldFont, PdfFont normalFont) {
        document.add(new Paragraph("\n\n"));

        Table footerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        // Valid until
        Cell leftCell = new Cell().setBorder(Border.NO_BORDER);
        if (prescription.getValidUntil() != null) {
            leftCell.add(new Paragraph("Valid Until: " + prescription.getValidUntil()
                    .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                    .setFont(normalFont).setFontSize(9));
        }

        // Doctor signature
        Cell rightCell = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);
        rightCell.add(new Paragraph("\n\n_______________________")
                .setFont(normalFont).setFontSize(10));
        rightCell.add(new Paragraph(prescription.getDoctorName() != null ? 
                "Dr. " + prescription.getDoctorName() : "Doctor's Signature")
                .setFont(boldFont).setFontSize(10));
        if (prescription.getDoctorSpecialization() != null) {
            rightCell.add(new Paragraph(prescription.getDoctorSpecialization())
                    .setFont(normalFont).setFontSize(9));
        }
        if (prescription.getDoctorLicenseNo() != null) {
            rightCell.add(new Paragraph("Reg. No: " + prescription.getDoctorLicenseNo())
                    .setFont(normalFont).setFontSize(9));
        }

        footerTable.addCell(leftCell);
        footerTable.addCell(rightCell);
        document.add(footerTable);

        // Generated info
        document.add(new Paragraph("\n─".repeat(80))
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY));
        document.add(new Paragraph("This is a computer-generated prescription.")
                .setFont(normalFont)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY));
    }
}

