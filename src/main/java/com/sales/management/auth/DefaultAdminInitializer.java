package com.sales.management.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DefaultAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.enabled:true}")
    private boolean defaultAdminEnabled;

    @Value("${app.default-admin.username:admin1}")
    private String defaultAdminUsername;

    @Value("${app.default-admin.password:password123}")
    private String defaultAdminPassword;

    public DefaultAdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!defaultAdminEnabled) {
            return;
        }
        if (defaultAdminUsername == null || defaultAdminUsername.isBlank()) {
            log.warn("Skip default admin initialization because username is blank");
            return;
        }
        if (defaultAdminPassword == null || defaultAdminPassword.isBlank()) {
            log.warn("Skip default admin initialization because password is blank");
            return;
        }
        if (userRepository.existsByUsername(defaultAdminUsername)) {
            return;
        }

        AppUser user = new AppUser();
        user.setUsername(defaultAdminUsername);
        user.setPassword(passwordEncoder.encode(defaultAdminPassword));
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        userRepository.save(user);

        log.warn("Created default admin account '{}'. Change password immediately for production.", defaultAdminUsername);
    }
}
