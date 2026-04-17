-- EMR Tables Migration
-- V1: Create EMR/EHR and E-Prescription tables

-- Patient Visits (Consultations/Encounters)
CREATE TABLE IF NOT EXISTS patient_visits (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    appointment_id BIGINT,
    doctor_id VARCHAR(100) NOT NULL,
    doctor_name VARCHAR(200),
    visit_datetime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    visit_type VARCHAR(50) NOT NULL DEFAULT 'CONSULTATION',
    chief_complaint TEXT,
    present_illness TEXT,
    past_medical_history TEXT,
    family_history TEXT,
    social_history TEXT,
    allergies TEXT,
    vital_signs JSONB,
    physical_examination TEXT,
    clinical_notes TEXT,
    treatment_plan TEXT,
    follow_up_instructions TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_visits_clinic ON patient_visits(clinic_id);
CREATE INDEX idx_visits_patient ON patient_visits(clinic_id, patient_id);
CREATE INDEX idx_visits_doctor ON patient_visits(clinic_id, doctor_id);
CREATE INDEX idx_visits_datetime ON patient_visits(clinic_id, visit_datetime DESC);

-- Diagnoses
CREATE TABLE IF NOT EXISTS diagnoses (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    visit_id BIGINT NOT NULL REFERENCES patient_visits(id) ON DELETE CASCADE,
    patient_id BIGINT NOT NULL,
    icd_code VARCHAR(20),
    diagnosis_name VARCHAR(500) NOT NULL,
    diagnosis_type VARCHAR(20) NOT NULL DEFAULT 'PRIMARY',
    severity VARCHAR(20),
    onset_date DATE,
    resolution_date DATE,
    is_chronic BOOLEAN DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_diagnoses_clinic ON diagnoses(clinic_id);
CREATE INDEX idx_diagnoses_visit ON diagnoses(visit_id);
CREATE INDEX idx_diagnoses_patient ON diagnoses(clinic_id, patient_id);
CREATE INDEX idx_diagnoses_icd ON diagnoses(icd_code);

-- Drug Master (Drug Catalog for auto-suggestions)
CREATE TABLE IF NOT EXISTS drugs_master (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50),
    drug_code VARCHAR(50),
    brand_name VARCHAR(200) NOT NULL,
    generic_name VARCHAR(200) NOT NULL,
    strength VARCHAR(100),
    form VARCHAR(50) NOT NULL,
    manufacturer VARCHAR(200),
    category VARCHAR(100),
    schedule_type VARCHAR(20),
    unit_price DECIMAL(10,2),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_drugs_clinic ON drugs_master(clinic_id);
CREATE INDEX idx_drugs_name ON drugs_master(brand_name, generic_name);
CREATE INDEX idx_drugs_generic ON drugs_master(generic_name);
CREATE INDEX idx_drugs_active ON drugs_master(is_active);

-- Prescriptions
CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    prescription_number VARCHAR(50) NOT NULL,
    visit_id BIGINT REFERENCES patient_visits(id),
    patient_id BIGINT NOT NULL,
    doctor_id VARCHAR(100) NOT NULL,
    doctor_name VARCHAR(200),
    doctor_license_no VARCHAR(100),
    doctor_specialization VARCHAR(200),
    prescription_date DATE NOT NULL DEFAULT CURRENT_DATE,
    valid_until DATE,
    diagnosis_summary TEXT,
    special_instructions TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    delivery_method VARCHAR(30),
    delivered_at TIMESTAMP,
    delivery_reference VARCHAR(200),
    pdf_file_path VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(clinic_id, prescription_number)
);

CREATE INDEX idx_prescriptions_clinic ON prescriptions(clinic_id);
CREATE INDEX idx_prescriptions_patient ON prescriptions(clinic_id, patient_id);
CREATE INDEX idx_prescriptions_visit ON prescriptions(visit_id);
CREATE INDEX idx_prescriptions_doctor ON prescriptions(clinic_id, doctor_id);
CREATE INDEX idx_prescriptions_date ON prescriptions(clinic_id, prescription_date DESC);
CREATE INDEX idx_prescriptions_status ON prescriptions(clinic_id, status);

-- Prescription Items (Medications)
CREATE TABLE IF NOT EXISTS prescription_items (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    drug_id BIGINT REFERENCES drugs_master(id),
    drug_name VARCHAR(200) NOT NULL,
    generic_name VARCHAR(200),
    strength VARCHAR(100),
    form VARCHAR(50),
    dosage VARCHAR(100) NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    duration VARCHAR(100) NOT NULL,
    quantity INT,
    route VARCHAR(50),
    timing VARCHAR(100),
    before_after_food VARCHAR(20),
    special_instructions TEXT,
    sequence_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prescription_items_prescription ON prescription_items(prescription_id);
CREATE INDEX idx_prescription_items_drug ON prescription_items(drug_id);

-- Lab Reports
CREATE TABLE IF NOT EXISTS lab_reports (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    visit_id BIGINT REFERENCES patient_visits(id),
    patient_id BIGINT NOT NULL,
    report_number VARCHAR(50),
    test_category VARCHAR(100),
    test_name VARCHAR(200) NOT NULL,
    test_date DATE NOT NULL,
    result_date DATE,
    lab_name VARCHAR(200),
    ordering_doctor VARCHAR(200),
    result_summary TEXT,
    result_values JSONB,
    interpretation VARCHAR(50),
    reference_range TEXT,
    is_abnormal BOOLEAN DEFAULT FALSE,
    notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lab_reports_clinic ON lab_reports(clinic_id);
CREATE INDEX idx_lab_reports_visit ON lab_reports(visit_id);
CREATE INDEX idx_lab_reports_patient ON lab_reports(clinic_id, patient_id);
CREATE INDEX idx_lab_reports_date ON lab_reports(clinic_id, test_date DESC);

-- File Attachments (X-rays, Scans, Documents)
CREATE TABLE IF NOT EXISTS file_attachments (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    visit_id BIGINT REFERENCES patient_visits(id),
    lab_report_id BIGINT REFERENCES lab_reports(id),
    file_category VARCHAR(50) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    storage_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    content_type VARCHAR(100),
    file_size BIGINT,
    checksum VARCHAR(64),
    description TEXT,
    tags TEXT[],
    uploaded_by VARCHAR(100),
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_files_clinic ON file_attachments(clinic_id);
CREATE INDEX idx_files_patient ON file_attachments(clinic_id, patient_id);
CREATE INDEX idx_files_visit ON file_attachments(visit_id);
CREATE INDEX idx_files_category ON file_attachments(clinic_id, file_category);
CREATE INDEX idx_files_deleted ON file_attachments(is_deleted);

-- Allergy Records
CREATE TABLE IF NOT EXISTS patient_allergies (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    allergen_type VARCHAR(50) NOT NULL,
    allergen_name VARCHAR(200) NOT NULL,
    reaction_type VARCHAR(100),
    severity VARCHAR(20),
    onset_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    notes TEXT,
    recorded_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_allergies_clinic ON patient_allergies(clinic_id);
CREATE INDEX idx_allergies_patient ON patient_allergies(clinic_id, patient_id);
CREATE INDEX idx_allergies_active ON patient_allergies(is_active);

-- Vital Signs History
CREATE TABLE IF NOT EXISTS vital_signs (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    visit_id BIGINT REFERENCES patient_visits(id),
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    blood_pressure_systolic INT,
    blood_pressure_diastolic INT,
    heart_rate INT,
    respiratory_rate INT,
    temperature DECIMAL(4,1),
    temperature_unit VARCHAR(1) DEFAULT 'C',
    oxygen_saturation DECIMAL(4,1),
    weight DECIMAL(5,1),
    weight_unit VARCHAR(2) DEFAULT 'kg',
    height DECIMAL(5,1),
    height_unit VARCHAR(2) DEFAULT 'cm',
    bmi DECIMAL(4,1),
    blood_sugar DECIMAL(5,1),
    notes TEXT,
    recorded_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vitals_clinic ON vital_signs(clinic_id);
CREATE INDEX idx_vitals_patient ON vital_signs(clinic_id, patient_id);
CREATE INDEX idx_vitals_visit ON vital_signs(visit_id);
CREATE INDEX idx_vitals_date ON vital_signs(clinic_id, recorded_at DESC);

-- Insert sample drugs for auto-suggestions
INSERT INTO drugs_master (clinic_id, drug_code, brand_name, generic_name, strength, form, category, is_active) VALUES
(NULL, 'PARA500', 'Dolo 650', 'Paracetamol', '650mg', 'TABLET', 'Analgesic/Antipyretic', true),
(NULL, 'PARA500', 'Crocin', 'Paracetamol', '500mg', 'TABLET', 'Analgesic/Antipyretic', true),
(NULL, 'AMOX500', 'Mox 500', 'Amoxicillin', '500mg', 'CAPSULE', 'Antibiotic', true),
(NULL, 'AMOX500', 'Novamox', 'Amoxicillin', '500mg', 'CAPSULE', 'Antibiotic', true),
(NULL, 'AZIT500', 'Azithral 500', 'Azithromycin', '500mg', 'TABLET', 'Antibiotic', true),
(NULL, 'AZIT500', 'Zithromax', 'Azithromycin', '500mg', 'TABLET', 'Antibiotic', true),
(NULL, 'CETIZ10', 'Cetzine', 'Cetirizine', '10mg', 'TABLET', 'Antihistamine', true),
(NULL, 'CETIZ10', 'Zyrtec', 'Cetirizine', '10mg', 'TABLET', 'Antihistamine', true),
(NULL, 'OMEP20', 'Omez', 'Omeprazole', '20mg', 'CAPSULE', 'PPI/Antacid', true),
(NULL, 'PANT40', 'Pan 40', 'Pantoprazole', '40mg', 'TABLET', 'PPI/Antacid', true),
(NULL, 'MONT10', 'Montair LC', 'Montelukast + Levocetirizine', '10mg+5mg', 'TABLET', 'Antihistamine', true),
(NULL, 'METF500', 'Glycomet', 'Metformin', '500mg', 'TABLET', 'Antidiabetic', true),
(NULL, 'METF850', 'Glycomet', 'Metformin', '850mg', 'TABLET', 'Antidiabetic', true),
(NULL, 'ATOV10', 'Atorva 10', 'Atorvastatin', '10mg', 'TABLET', 'Statin', true),
(NULL, 'LOSART50', 'Losar 50', 'Losartan', '50mg', 'TABLET', 'Antihypertensive', true),
(NULL, 'AMLO5', 'Amlong 5', 'Amlodipine', '5mg', 'TABLET', 'Antihypertensive', true),
(NULL, 'IBU400', 'Brufen 400', 'Ibuprofen', '400mg', 'TABLET', 'NSAID', true),
(NULL, 'DICLO50', 'Voveran 50', 'Diclofenac', '50mg', 'TABLET', 'NSAID', true),
(NULL, 'PRED5', 'Wysolone 5', 'Prednisolone', '5mg', 'TABLET', 'Corticosteroid', true),
(NULL, 'LEVO500', 'Levomac 500', 'Levofloxacin', '500mg', 'TABLET', 'Antibiotic', true),
(NULL, 'CIPRO500', 'Ciplox 500', 'Ciprofloxacin', '500mg', 'TABLET', 'Antibiotic', true),
(NULL, 'DOM10', 'Domstal', 'Domperidone', '10mg', 'TABLET', 'Antiemetic', true),
(NULL, 'ONDANS4', 'Emeset 4', 'Ondansetron', '4mg', 'TABLET', 'Antiemetic', true),
(NULL, 'RABEP20', 'Razo 20', 'Rabeprazole', '20mg', 'TABLET', 'PPI/Antacid', true),
(NULL, 'CLAV625', 'Augmentin 625', 'Amoxicillin + Clavulanic Acid', '500mg+125mg', 'TABLET', 'Antibiotic', true),
(NULL, 'CEFX500', 'Cefixime 500', 'Cefixime', '500mg', 'TABLET', 'Antibiotic', true),
(NULL, 'VITAMIN_D3', 'D3 Must', 'Cholecalciferol', '60000IU', 'SOFTGEL', 'Vitamin', true),
(NULL, 'MULTIVIT', 'Supradyn', 'Multivitamin', '', 'TABLET', 'Vitamin', true),
(NULL, 'B12', 'Neurobion Forte', 'Vitamin B Complex', '', 'TABLET', 'Vitamin', true),
(NULL, 'IRON', 'Fefol', 'Ferrous Sulphate + Folic Acid', '', 'CAPSULE', 'Iron Supplement', true)
ON CONFLICT DO NOTHING;

