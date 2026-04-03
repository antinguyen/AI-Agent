ALTER TABLE user_preferences
    ADD COLUMN order_list_preset_key VARCHAR(32) NOT NULL DEFAULT 'ALL';

ALTER TABLE user_preferences
    ADD COLUMN order_list_status_filter VARCHAR(16) NOT NULL DEFAULT '';

ALTER TABLE user_preferences
    ADD COLUMN order_list_fulfillment_filter VARCHAR(32) NOT NULL DEFAULT 'ALL';

ALTER TABLE user_preferences
    ADD CONSTRAINT chk_user_preferences_order_list_preset_key
        CHECK (order_list_preset_key IN ('ALL', 'PENDING_CONFIRMATION', 'READY_TO_SHIP', 'PAID', 'RETURNED', 'CANCELLED', 'CUSTOM'));

ALTER TABLE user_preferences
    ADD CONSTRAINT chk_user_preferences_order_list_status_filter
        CHECK (order_list_status_filter IN ('', 'CREATED', 'CONFIRMED', 'PAID', 'RETURNED', 'CANCELLED'));

ALTER TABLE user_preferences
    ADD CONSTRAINT chk_user_preferences_order_list_fulfillment_filter
        CHECK (order_list_fulfillment_filter IN ('ALL', 'PENDING', 'READY_TO_SHIP', 'SHIPPED', 'SHIPMENT_CANCELLED', 'CANCELLED'));