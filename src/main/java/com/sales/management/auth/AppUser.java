package com.sales.management.auth;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "app_users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_users_username", columnNames = "username")
})
public class AppUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(length = 64)
    private String refreshToken;

    @Column
    private Instant refreshTokenExpiresAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public String generateRefreshToken(long ttlSeconds) {
        this.refreshToken = UUID.randomUUID().toString().replace("-", "");
        this.refreshTokenExpiresAt = Instant.now().plusSeconds(ttlSeconds);
        return this.refreshToken;
    }

    public boolean isRefreshTokenValid(String token) {
        return token != null
                && token.equals(this.refreshToken)
                && this.refreshTokenExpiresAt != null
                && Instant.now().isBefore(this.refreshTokenExpiresAt);
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
        this.refreshTokenExpiresAt = null;
    }

    // UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return active; }

    // Getters/setters
    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }

    @Override
    public String getUsername()            { return username; }
    public void setUsername(String u)      { this.username = u; }

    @Override
    public String getPassword()            { return password; }
    public void setPassword(String p)      { this.password = p; }

    public UserRole getRole()              { return role; }
    public void setRole(UserRole role)     { this.role = role; }

    public boolean isActive()              { return active; }
    public void setActive(boolean active)  { this.active = active; }

    public Instant getCreatedAt()          { return createdAt; }

    public String getRefreshToken()                            { return refreshToken; }
    public Instant getRefreshTokenExpiresAt()                  { return refreshTokenExpiresAt; }
}
