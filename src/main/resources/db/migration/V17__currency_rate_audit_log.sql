-- Audit log for currency exchange rate changes (UPSERT and RESET actions)
CREATE TABLE currency_rate_audit_log (
    id BIGSERIAL PRIMARY KEY,
    currency_code VARCHAR(3) NOT NULL,
    old_bank_name VARCHAR(128),
    new_bank_name VARCHAR(128) NOT NULL,
    old_rate DECIMAL(18, 6),
    new_rate DECIMAL(18, 6) NOT NULL,
    action VARCHAR(16) NOT NULL,
    changed_by VARCHAR(64),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_currency_rate_audit_code ON currency_rate_audit_log(currency_code);
CREATE INDEX idx_currency_rate_audit_time ON currency_rate_audit_log(changed_at DESC);
