-- Who viewed or changed which record. Holds record IDs only, never patient names or clinical text.
CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    action VARCHAR(30) NOT NULL,
    resource_type VARCHAR(40) NOT NULL,
    resource_id VARCHAR(40),
    detail VARCHAR(60),
    http_method VARCHAR(10),
    path VARCHAR(300),
    status INTEGER,
    ip_address VARCHAR(64),
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_clinic_time ON audit_events (clinic_id, occurred_at DESC);
CREATE INDEX idx_audit_clinic_resource ON audit_events (clinic_id, resource_type, resource_id);
CREATE INDEX idx_audit_clinic_actor ON audit_events (clinic_id, actor, occurred_at DESC);
