CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    performed_by UUID NOT NULL REFERENCES users(id),
    performed_at TIMESTAMP DEFAULT NOW(),
    ip_address VARCHAR(45)
);

CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_performed_by ON audit_log(performed_by);
CREATE INDEX idx_audit_log_performed_at ON audit_log(performed_at);
