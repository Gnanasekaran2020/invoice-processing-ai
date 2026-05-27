-- V3: Add notifications and audit_log tables
-- ─────────────────────────────────────────────────────────────────────────────

-- ENUM-like check constraints (DB-level guard, app uses Java enums)
-- notifications
CREATE TABLE IF NOT EXISTS notifications (
    notif_id        BIGSERIAL       PRIMARY KEY,
    user_id         INTEGER         NOT NULL,
    invoice_id      INTEGER,
    channel         VARCHAR(20)     NOT NULL DEFAULT 'EMAIL'
                        CONSTRAINT chk_notif_channel
                        CHECK (channel IN ('EMAIL','SMS','PUSH','WEBHOOK')),
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                        CONSTRAINT chk_notif_status
                        CHECK (status IN ('PENDING','SENT','FAILED','RETRYING','SUPPRESSED')),
    template        VARCHAR(100),
    ses_msg_id      VARCHAR(255),
    sent_at         TIMESTAMP,
    payload         JSONB,
    created_by      VARCHAR(100),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    retry_count     INTEGER         NOT NULL DEFAULT 0,
    error_message   TEXT,

    CONSTRAINT fk_notif_user
        FOREIGN KEY (user_id)    REFERENCES users(id)     ON DELETE RESTRICT,
    CONSTRAINT fk_notif_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_notif_user_id    ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notif_invoice_id ON notifications (invoice_id);
CREATE INDEX IF NOT EXISTS idx_notif_status     ON notifications (status);
CREATE INDEX IF NOT EXISTS idx_notif_sent_at    ON notifications (sent_at);
CREATE INDEX IF NOT EXISTS idx_notif_channel    ON notifications (channel);

-- ─────────────────────────────────────────────────────────────────────────────
-- audit_log (append-only — no UPDATE / DELETE allowed via application)
CREATE TABLE IF NOT EXISTS audit_log (
    audit_id        BIGSERIAL       PRIMARY KEY,
    user_id         INTEGER,                          -- NULL for system/background events
    action          VARCHAR(60)     NOT NULL,
    entity          VARCHAR(100)    NOT NULL,
    entity_id       VARCHAR(100),
    meta_data       JSONB,
    source_ip       VARCHAR(45),
    correction_id   BIGINT,                           -- references audit_log.audit_id
    occurred_at     TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_correction
        FOREIGN KEY (correction_id) REFERENCES audit_log(audit_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_user_id     ON audit_log (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity      ON audit_log (entity, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_action      ON audit_log (action);
CREATE INDEX IF NOT EXISTS idx_audit_occurred_at ON audit_log (occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_source_ip   ON audit_log (source_ip);
CREATE INDEX IF NOT EXISTS idx_audit_correction  ON audit_log (correction_id);

-- Prevent anyone from updating or deleting audit rows at DB level
-- (PostgreSQL row-level security alternative — use a trigger)
CREATE OR REPLACE FUNCTION fn_audit_log_immutable()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_log rows are immutable — corrections must be new rows';
END;
$$;

CREATE TRIGGER trg_audit_log_no_update
    BEFORE UPDATE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION fn_audit_log_immutable();

CREATE TRIGGER trg_audit_log_no_delete
    BEFORE DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION fn_audit_log_immutable();

