package com.sales.management.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionSafetyValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionSafetyValidator.class);
    private static final String DEFAULT_JWT_SECRET = "your-secret-key-change-in-production";
    private static final String DEFAULT_ADMIN_PASSWORD = "password123";

    private final String jwtSecret;
    private final boolean defaultAdminEnabled;
    private final String defaultAdminUsername;
    private final String defaultAdminPassword;
    private final boolean mailEnabled;

    public ProductionSafetyValidator(
            @Value("${app.jwt.secret:}") String jwtSecret,
            @Value("${app.default-admin.enabled:true}") boolean defaultAdminEnabled,
            @Value("${app.default-admin.username:admin1}") String defaultAdminUsername,
            @Value("${app.default-admin.password:password123}") String defaultAdminPassword,
            @Value("${app.mail.enabled:true}") boolean mailEnabled
    ) {
        this.jwtSecret = jwtSecret;
        this.defaultAdminEnabled = defaultAdminEnabled;
        this.defaultAdminUsername = defaultAdminUsername;
        this.defaultAdminPassword = defaultAdminPassword;
        this.mailEnabled = mailEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (jwtSecret == null || jwtSecret.isBlank() || DEFAULT_JWT_SECRET.equals(jwtSecret) || jwtSecret.length() < 32) {
            log.warn("[SECURITY] app.jwt.secret is weak or default. Configure APP_JWT_SECRET with a strong value in production.");
        }

        if (defaultAdminEnabled && DEFAULT_ADMIN_PASSWORD.equals(defaultAdminPassword)) {
            log.warn("[SECURITY] Default admin bootstrap is enabled with default password for user '{}'. Change APP_DEFAULT_ADMIN_PASSWORD immediately.", defaultAdminUsername);
        }

        if (!mailEnabled) {
            log.warn("[OPERATIONS] app.mail.enabled=false in prod profile. Outbound email notifications are disabled.");
        }
    }
}