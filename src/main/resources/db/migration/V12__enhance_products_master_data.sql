ALTER TABLE products ADD COLUMN unit VARCHAR(32) NOT NULL DEFAULT 'pcs';
ALTER TABLE products ADD COLUMN purchase_price DECIMAL(15, 2);
ALTER TABLE products ADD COLUMN currency_code VARCHAR(3) NOT NULL DEFAULT 'VND';
ALTER TABLE products ADD COLUMN exchange_rate DECIMAL(18, 6) NOT NULL DEFAULT 1;
ALTER TABLE products ADD COLUMN image_url VARCHAR(512);
ALTER TABLE products ADD COLUMN supplier VARCHAR(255);
ALTER TABLE products ADD COLUMN brand VARCHAR(255);
ALTER TABLE products ADD COLUMN origin_country VARCHAR(128);
ALTER TABLE products ADD COLUMN manufacture_year INTEGER;

CREATE INDEX idx_products_brand ON products(brand);
CREATE INDEX idx_products_supplier ON products(supplier);
CREATE INDEX idx_products_currency_code ON products(currency_code);
