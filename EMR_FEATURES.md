# EMR/EHR and E-Prescription Module

## Overview

The `clinic-emr` microservice provides comprehensive Electronic Medical Records (EMR/EHR) and E-Prescription functionality for the Clinical Management System.

## Features

### 1. Electronic Medical Records (EMR/EHR)

#### Patient Visits/Consultations
- Create, update, and complete patient visits
- Record chief complaints, present illness, medical history
- Capture physical examination findings
- Store clinical notes and treatment plans
- Track follow-up instructions

#### Diagnoses
- Add multiple diagnoses per visit (PRIMARY, SECONDARY, DIFFERENTIAL)
- ICD code support
- Track chronic conditions
- Record onset and resolution dates

#### Vital Signs
- Blood pressure (systolic/diastolic)
- Heart rate, respiratory rate
- Temperature, oxygen saturation
- Weight, height, BMI
- Blood sugar levels

#### Patient History Timeline
- Aggregated view of all patient records
- Recent visits, diagnoses, prescriptions
- Lab reports and file attachments
- Latest vital signs

### 2. E-Prescription

#### Prescription Management
- Create prescriptions with multiple medications
- Draft → Finalize → Deliver workflow
- Auto-generate prescription numbers
- Link prescriptions to visits

#### Drug Auto-Suggestions
- Pre-loaded drug catalog with common Indian medications
- Search by brand name or generic name
- Drug strength, form, and category information

#### Prescription Delivery
- **Print**: Generate PDF for printing
- **Email**: Send PDF via email
- **WhatsApp**: Generate WhatsApp deep link with prescription summary
- **SMS**: Send prescription notification

#### PDF Generation
- Professional prescription layout with clinic letterhead
- Doctor information and signature area
- Medication table with dosage, frequency, duration
- Special instructions and validity date

### 3. Lab Reports

- Create and manage laboratory test reports
- Track test status (PENDING, IN_PROGRESS, COMPLETED)
- Store result values as structured JSON
- Mark abnormal results
- Link to patient visits

### 4. File Attachments

- Upload X-rays, CT scans, MRI images
- Store documents (consent forms, insurance, referrals)
- Support multiple file categories
- Soft delete functionality
- Local file storage (GCS support ready)

## API Endpoints

### Visits
```
POST   /api/v1/emr/visits              - Create visit
GET    /api/v1/emr/visits/{id}         - Get visit details
PUT    /api/v1/emr/visits/{id}         - Update visit
POST   /api/v1/emr/visits/{id}/complete - Complete visit
GET    /api/v1/emr/visits/patient/{id} - Get patient visits
GET    /api/v1/emr/visits              - List all visits
```

### Prescriptions
```
POST   /api/v1/emr/prescriptions                - Create prescription
GET    /api/v1/emr/prescriptions/{id}           - Get prescription
POST   /api/v1/emr/prescriptions/{id}/finalize  - Finalize prescription
POST   /api/v1/emr/prescriptions/{id}/deliver   - Deliver prescription
GET    /api/v1/emr/prescriptions/patient/{id}   - Patient prescriptions
GET    /api/v1/emr/prescriptions/drugs/search?q= - Drug search
```

### Diagnoses
```
POST   /api/v1/emr/diagnoses                    - Add diagnosis
GET    /api/v1/emr/visits/{id}/diagnoses        - Visit diagnoses
GET    /api/v1/emr/patients/{id}/diagnoses      - Patient diagnoses
```

### Lab Reports
```
POST   /api/v1/emr/lab-reports                  - Create lab report
GET    /api/v1/emr/lab-reports/{id}             - Get lab report
PUT    /api/v1/emr/lab-reports/{id}             - Update lab report
GET    /api/v1/emr/lab-reports/patient/{id}     - Patient lab reports
```

### Files
```
POST   /api/v1/emr/files/upload         - Upload file
GET    /api/v1/emr/files/{id}           - Get file info
GET    /api/v1/emr/files/{id}/download  - Download file
DELETE /api/v1/emr/files/{id}           - Delete file
GET    /api/v1/emr/files/patient/{id}   - Patient files
```

### Patient History
```
GET    /api/v1/emr/patients/{id}/history - Complete patient history
```

## Database Tables

- `patient_visits` - Consultation/encounter records
- `diagnoses` - Patient diagnoses
- `prescriptions` - E-prescriptions
- `prescription_items` - Prescription medications
- `drugs_master` - Drug catalog
- `lab_reports` - Laboratory test results
- `file_attachments` - X-rays, scans, documents
- `vital_signs` - Patient vital signs history
- `patient_allergies` - Allergy records

## Configuration

```yaml
server:
  port: 8086

app:
  file-storage:
    type: local
    local:
      base-path: ./uploads
  services:
    notification-url: http://localhost:8084
```

## Docker

The EMR service runs on port 8086 and requires:
- PostgreSQL database
- Java 21 runtime
- Volume mount for file uploads

```bash
docker-compose up -d clinic-emr
```

## Pre-loaded Drug Catalog

Includes common Indian medications:
- Dolo 650, Crocin (Paracetamol)
- Mox 500, Augmentin (Antibiotics)
- Azithral 500, Zithromax (Azithromycin)
- Cetzine, Zyrtec (Antihistamines)
- Omez, Pan 40 (PPIs)
- Glycomet (Metformin)
- Atorva, Losar, Amlong (Cardiac)
- Vitamin supplements and more

