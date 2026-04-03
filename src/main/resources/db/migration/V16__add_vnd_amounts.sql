-- Add VND-converted amount to sales_orders for multi-currency revenue reporting
ALTER TABLE sales_orders
    ADD COLUMN total_vnd DECIMAL(18, 2);

-- Add VND-converted amount to payments for aggregated revenue queries
ALTER TABLE payments
    ADD COLUMN vnd_amount DECIMAL(18, 2);
