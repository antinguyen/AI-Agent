package com.sales.management.auth;

import com.sales.management.auth.dto.AuthResponse;
import com.sales.management.auth.dto.LoginRequest;
import com.sales.management.auth.dto.RefreshTokenRequest;
import com.sales.management.auth.dto.RegisterRequest;
import com.sales.management.common.exception.BusinessRuleException;
import com.sales.management.common.exception.DuplicateResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService implements UserDetailsService {

    private static final long REFRESH_TOKEN_TTL_SECONDS = 7L * 24 * 3600; // 7 days

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Lazy
    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken: " + request.username());
        }

        UserRole role;
        try {
            role = UserRole.valueOf(request.role());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Invalid role: " + request.role());
        }

        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setActive(true);
        String refreshToken = user.generateRefreshToken(REFRESH_TOKEN_TTL_SECONDS);
        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getUsername(), role.name());
        return new AuthResponse(token, refreshToken, user.getUsername(), role.name());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            throw new BusinessRuleException("Invalid username or password");
        }

        AppUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.username()));

        String refreshToken = user.generateRefreshToken(REFRESH_TOKEN_TTL_SECONDS);
        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, refreshToken, user.getUsername(), user.getRole().name());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        userRepository.findByRefreshToken(request.refreshToken())
                .ifPresent(user -> {
                    user.clearRefreshToken();
                    userRepository.save(user);
                });
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        AppUser user = userRepository.findByRefreshToken(request.refreshToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (!user.isRefreshTokenValid(request.refreshToken())) {
            user.clearRefreshToken();
            userRepository.save(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        // Rotate refresh token
        String newRefreshToken = user.generateRefreshToken(REFRESH_TOKEN_TTL_SECONDS);
        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, newRefreshToken, user.getUsername(), user.getRole().name());
    }
}
