CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    locale VARCHAR(16) NOT NULL DEFAULT 'vi-VN',
    currency_code VARCHAR(3) NOT NULL DEFAULT 'VND',
    reduced_motion BOOLEAN NOT NULL DEFAULT FALSE,
    default_landing_page VARCHAR(64) NOT NULL DEFAULT '/orders',
    table_page_size INT NOT NULL DEFAULT 15,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT chk_user_preferences_currency_code CHECK (char_length(currency_code) = 3 AND upper(currency_code) = currency_code),
    CONSTRAINT chk_user_preferences_table_page_size CHECK (table_page_size BETWEEN 5 AND 100)
);

CREATE INDEX idx_user_preferences_updated_at ON user_preferences(updated_at DESC);
