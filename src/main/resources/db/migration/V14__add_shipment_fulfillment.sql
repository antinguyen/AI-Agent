-- V14__add_shipment_fulfillment.sql

CREATE TABLE shipments (
    id BIGSERIAL PRIMARY KEY,
    shipment_number VARCHAR(64) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL UNIQUE,
    warehouse_id BIGINT,
    status VARCHAR(16) NOT NULL,
    note VARCHAR(512),
    shipped_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES sales_orders(id),
    CONSTRAINT fk_shipments_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

CREATE TABLE shipment_items (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shipment_items_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE,
    CONSTRAINT fk_shipment_items_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    CONSTRAINT fk_shipment_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE UNIQUE INDEX uk_shipment_items_order_item ON shipment_items(order_item_id);
CREATE INDEX idx_shipments_status ON shipments(status);
CREATE INDEX idx_shipments_created_at ON shipments(created_at);
CREATE INDEX idx_shipment_items_shipment_id ON shipment_items(shipment_id);
