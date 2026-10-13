-- V3__enhance_billing.sql

-- ============================================================
-- Extend invoices for itemized billing
-- ============================================================

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS subtotal_amount DECIMAL(12,2);

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0;

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0;

UPDATE invoices
SET subtotal_amount = total_amount
WHERE subtotal_amount IS NULL;

ALTER TABLE invoices
    ALTER COLUMN subtotal_amount SET NOT NULL;

-- ============================================================
-- Invoice Items
-- ============================================================

CREATE TABLE IF NOT EXISTS invoice_items (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    clinic_id VARCHAR(50) NOT NULL,
    invoice_id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    item_type VARCHAR(50),
    quantity DECIMAL(10,2) NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    tax_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_invoice_items_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoices(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_invoice_item_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_invoice_item_unit_price
        CHECK (unit_price >= 0),

    CONSTRAINT chk_invoice_item_discount
        CHECK (discount_amount >= 0),

    CONSTRAINT chk_invoice_item_tax_percent
        CHECK (tax_percent >= 0),

    CONSTRAINT chk_invoice_item_tax_amount
        CHECK (tax_amount >= 0),

    CONSTRAINT chk_invoice_item_total
        CHECK (total_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_invoice_items_clinic_invoice
    ON invoice_items(clinic_id, invoice_id);

CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice
    ON invoice_items(invoice_id);

-- ============================================================
-- Payments
-- ============================================================

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    clinic_id VARCHAR(50) NOT NULL,
    invoice_id BIGINT NOT NULL,
    payment_number VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(30) NOT NULL,
    transaction_reference VARCHAR(100),
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payments_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoices(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_payment_amount_positive
        CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_payments_clinic_invoice
    ON payments(clinic_id, invoice_id);

CREATE INDEX IF NOT EXISTS idx_payments_invoice_date
    ON payments(invoice_id, payment_date);

CREATE INDEX IF NOT EXISTS idx_payments_clinic_date
    ON payments(clinic_id, payment_date);

CREATE INDEX IF NOT EXISTS idx_payments_transaction_reference
    ON payments(clinic_id, transaction_reference);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_clinic_payment_number
    ON payments(clinic_id, payment_number);
