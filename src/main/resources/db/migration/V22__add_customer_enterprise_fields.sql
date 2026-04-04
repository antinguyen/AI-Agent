ALTER TABLE customers
    ADD COLUMN address VARCHAR(500),
    ADD COLUMN tax_code VARCHAR(32),
    ADD COLUMN legal_representative VARCHAR(255),
    ADD COLUMN contact_person VARCHAR(255);
