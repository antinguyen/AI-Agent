ALTER TABLE app_users ADD COLUMN refresh_token VARCHAR(64);
ALTER TABLE app_users ADD COLUMN refresh_token_expires_at TIMESTAMP WITH TIME ZONE;
