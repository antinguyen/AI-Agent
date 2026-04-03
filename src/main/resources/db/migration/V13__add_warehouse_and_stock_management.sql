-- V13__add_warehouse_and_stock_management.sql
-- Create warehouse table
CREATE TABLE warehouses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(512),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64)
);

-- Create product_warehouse_stock mapping table
CREATE TABLE product_warehouse_stock (
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    low_stock_threshold INTEGER NOT NULL DEFAULT 10,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    last_count_at TIMESTAMP,
    PRIMARY KEY (product_id, warehouse_id),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
);

-- Create stock transaction history table for audit trail
CREATE TABLE stock_transactions (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    transaction_type VARCHAR(32) NOT NULL, -- INITIAL, ADJUSTMENT, PURCHASE, SALES, RETURN, RESERVATION, RELEASE
    quantity_change INTEGER NOT NULL,
    reference_type VARCHAR(32), -- ORDER, PURCHASE_ORDER, STOCK_COUNT
    reference_id BIGINT,
    notes VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- Add warehouse_id to sales_orders for tracking which warehouse fulfilled the order
ALTER TABLE sales_orders ADD COLUMN warehouse_id BIGINT;
ALTER TABLE sales_orders ADD CONSTRAINT fk_orders_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

-- Insert default warehouse (for backwards compatibility)
INSERT INTO warehouses (code, name, address, active)
VALUES ('WH-DEFAULT', 'Kho chính', 'Default Warehouse', TRUE);

-- Migrate existing stock_quantity to product_warehouse_stock
INSERT INTO product_warehouse_stock (product_id, warehouse_id, quantity, low_stock_threshold, reserved_quantity)
SELECT id, (SELECT id FROM warehouses WHERE code = 'WH-DEFAULT'), stock_quantity, low_stock_threshold, 0
FROM products;

-- Create indexes for performance
CREATE INDEX idx_warehouses_active ON warehouses(active);
CREATE INDEX idx_product_warehouse_stock_qty ON product_warehouse_stock(quantity);
CREATE INDEX idx_product_warehouse_stock_reserved ON product_warehouse_stock(reserved_quantity);
CREATE INDEX idx_stock_txn_product_warehouse ON stock_transactions(product_id, warehouse_id);
CREATE INDEX idx_stock_txn_type ON stock_transactions(transaction_type);
CREATE INDEX idx_stock_txn_created_at ON stock_transactions(created_at);
