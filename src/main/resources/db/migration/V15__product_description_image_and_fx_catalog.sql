ALTER TABLE products
    ADD COLUMN description VARCHAR(2000);

CREATE TABLE currency_exchange_rates (
    id BIGSERIAL PRIMARY KEY,
    currency_code VARCHAR(3) NOT NULL UNIQUE,
    bank_name VARCHAR(128) NOT NULL,
    rate_to_vnd DECIMAL(18, 6) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_currency_exchange_rates_code ON currency_exchange_rates(currency_code);

INSERT INTO currency_exchange_rates(currency_code, bank_name, rate_to_vnd)
SELECT 'VND', 'VIETCOMBANK', 1
WHERE NOT EXISTS (SELECT 1 FROM currency_exchange_rates WHERE currency_code = 'VND');

INSERT INTO currency_exchange_rates(currency_code, bank_name, rate_to_vnd)
SELECT 'USD', 'VIETCOMBANK', 25450
WHERE NOT EXISTS (SELECT 1 FROM currency_exchange_rates WHERE currency_code = 'USD');

INSERT INTO currency_exchange_rates(currency_code, bank_name, rate_to_vnd)
SELECT 'EUR', 'VIETCOMBANK', 27600
WHERE NOT EXISTS (SELECT 1 FROM currency_exchange_rates WHERE currency_code = 'EUR');

INSERT INTO currency_exchange_rates(currency_code, bank_name, rate_to_vnd)
SELECT 'JPY', 'VIETCOMBANK', 171
WHERE NOT EXISTS (SELECT 1 FROM currency_exchange_rates WHERE currency_code = 'JPY');
