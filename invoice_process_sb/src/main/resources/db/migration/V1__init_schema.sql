-- ============================================================
--  V1__init_schema.sql
--  Invoice Processing System — Database Schema
-- ============================================================

-- ── Users ─────────────────────────────────────────────────
CREATE TABLE users (
    id              SERIAL          PRIMARY KEY,
    email           VARCHAR(100)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    phone_number    VARCHAR(20)     NOT NULL,
    first_name      VARCHAR(50)     NOT NULL,
    last_name       VARCHAR(50)     NOT NULL
);

CREATE INDEX idx_users_email ON users(email);

-- ── Invoices ───────────────────────────────────────────────
CREATE TABLE invoices (
    invoice_id          SERIAL          PRIMARY KEY,
    user_id             INTEGER         NOT NULL REFERENCES users(id),
    invoice_number      VARCHAR(100),
    invoice_date        DATE,
    amount              NUMERIC(15,2),
    vendor_name         VARCHAR(255),
    vendor_address      TEXT,
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    comments            TEXT,

    -- AI processing metadata (internal — not exposed as columns to end-user)
    original_file_name  VARCHAR(512),
    file_type           VARCHAR(20),
    file_size_bytes     BIGINT,
    storage_path        VARCHAR(1024),
    storage_bucket      VARCHAR(255),
    processing_status   VARCHAR(30)     NOT NULL DEFAULT 'UPLOADED',
    processing_error    TEXT,
    ai_confidence_score NUMERIC(5,2),
    ai_model_used       VARCHAR(100),
    processing_duration_ms BIGINT,
    ocr_text            TEXT,
    reviewed_by         INTEGER         REFERENCES users(id),
    reviewed_at         TIMESTAMP,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoices_user_id           ON invoices(user_id);
CREATE INDEX idx_invoices_status            ON invoices(status);
CREATE INDEX idx_invoices_processing_status ON invoices(processing_status);
CREATE INDEX idx_invoices_vendor_name       ON invoices(vendor_name);
CREATE INDEX idx_invoices_invoice_date      ON invoices(invoice_date);
CREATE INDEX idx_invoices_invoice_number    ON invoices(invoice_number);
CREATE INDEX idx_invoices_created_at        ON invoices(created_at DESC);

-- ── Invoice Details ────────────────────────────────────────
CREATE TABLE invoice_details (
    detail_id           SERIAL          PRIMARY KEY,
    invoice_id          INTEGER         NOT NULL REFERENCES invoices(invoice_id) ON DELETE CASCADE,
    item_description    TEXT,
    quantity            NUMERIC(10,3),
    unit_price          NUMERIC(15,2),
    total_price         NUMERIC(15,2)
);

CREATE INDEX idx_invoice_details_invoice_id ON invoice_details(invoice_id);

-- ── Seed: Default Admin User ───────────────────────────────
-- Password: Admin@1234  (BCrypt — change in production!)
INSERT INTO users (email, password_hash, phone_number, first_name, last_name) VALUES
(
  'admin_u2a@cts.com',
  '$2a$12$Kp7lHa1qA2b3c4d5e6f7gOGm3vG1pR2sT4uV5wX6yZ7aB8cD9eF0g',
  '+10000000000',
  'System',
  'Administrator'
);
