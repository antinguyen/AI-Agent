ALTER TABLE products   ADD COLUMN created_by VARCHAR(64);
ALTER TABLE products   ADD COLUMN updated_by VARCHAR(64);

ALTER TABLE customers  ADD COLUMN created_by VARCHAR(64);
ALTER TABLE customers  ADD COLUMN updated_by VARCHAR(64);

ALTER TABLE sales_orders ADD COLUMN created_by VARCHAR(64);
ALTER TABLE sales_orders ADD COLUMN updated_by VARCHAR(64);
