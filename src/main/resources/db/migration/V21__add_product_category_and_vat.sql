ALTER TABLE products
    ADD COLUMN category VARCHAR(100) NOT NULL DEFAULT 'General';

ALTER TABLE products
    ADD COLUMN vat_rate DECIMAL(5, 2) NOT NULL DEFAULT 0;

CREATE INDEX idx_products_category ON products(category);
