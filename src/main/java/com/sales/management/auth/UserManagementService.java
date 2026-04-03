package com.sales.management.auth;

import com.sales.management.auth.dto.ChangeRoleRequest;
import com.sales.management.auth.dto.RegisterRequest;
import com.sales.management.auth.dto.ResetPasswordRequest;
import com.sales.management.auth.dto.UserResponse;
import com.sales.management.common.exception.DuplicateResourceException;
import com.sales.management.common.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken: " + request.username());
        }

        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.valueOf(request.role()));
        user.setActive(true);
        return toResponse(userRepository.save(user));
    }

    public Page<UserResponse> listUsers(Pageable pageable, String username, UserRole role, Boolean active) {
        pageable = Objects.requireNonNull(pageable, "pageable must not be null");

        Specification<AppUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (username != null && !username.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase(Locale.ROOT) + "%"));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional
    public UserResponse deactivateUser(Long id) {
        AppUser user = findOrThrow(id);
        user.setActive(false);
        user.clearRefreshToken();
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse activateUser(Long id) {
        AppUser user = findOrThrow(id);
        user.setActive(true);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse changeRole(Long id, ChangeRoleRequest request) {
        AppUser user = findOrThrow(id);
        user.setRole(UserRole.valueOf(request.role()));
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        id = Objects.requireNonNull(id, "id must not be null");

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public UserResponse resetPassword(Long id, ResetPasswordRequest request) {
        AppUser user = findOrThrow(id);
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.clearRefreshToken();
        return toResponse(userRepository.save(user));
    }

    private AppUser findOrThrow(Long id) {
        Long userId = Objects.requireNonNull(id, "id must not be null");

        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(),
                user.getRole().name(), user.isActive(), user.getCreatedAt());
    }
}
