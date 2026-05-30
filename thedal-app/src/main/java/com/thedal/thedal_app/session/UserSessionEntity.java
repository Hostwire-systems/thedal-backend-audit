package com.thedal.thedal_app.session;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_user_sessions_user_id", columnList = "user_id"),
    @Index(name = "idx_user_sessions_token_hash", columnList = "session_token_hash"),
    @Index(name = "idx_user_sessions_active", columnList = "is_active"),
    @Index(name = "idx_user_sessions_last_access", columnList = "last_access_time")
})
@Getter
@Setter
@NoArgsConstructor
public class UserSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private Long userId;

    @Column(name = "session_token_hash", nullable = false, length = 128)
    @JsonProperty("session_token_hash")
    private String sessionTokenHash; // Hashed JWT token for security

    @Column(name = "ip_address", length = 45)
    @JsonProperty("ip_address")
    private String ipAddress; // Supports both IPv4 and IPv6

    @Column(name = "user_agent", length = 500)
    @JsonProperty("user_agent")
    private String userAgent;

    @Column(name = "device_type", length = 50)
    @JsonProperty("device_type")
    private String deviceType; // Mobile, Desktop, Tablet

    @Column(name = "browser_name", length = 100)
    @JsonProperty("browser_name")
    private String browserName;

    @Column(name = "operating_system", length = 100)
    @JsonProperty("operating_system")
    private String operatingSystem;

    @Column(name = "location_country", length = 100)
    @JsonProperty("location_country")
    private String locationCountry;

    @Column(name = "location_city", length = 100)
    @JsonProperty("location_city")
    private String locationCity;

    @Column(name = "location_region", length = 100)
    @JsonProperty("location_region")
    private String locationRegion;

    @Column(name = "login_time", nullable = false)
    @JsonProperty("login_time")
    private LocalDateTime loginTime;

    @Column(name = "last_access_time", nullable = false)
    @JsonProperty("last_access_time")
    private LocalDateTime lastAccessTime;

    @Column(name = "expires_at")
    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    @JsonProperty("is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public UserSessionEntity(Long userId, String sessionTokenHash, String ipAddress, 
                           String userAgent, LocalDateTime expiresAt) {
        this.userId = userId;
        this.sessionTokenHash = sessionTokenHash;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.loginTime = LocalDateTime.now();
        this.lastAccessTime = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.isActive = true;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (loginTime == null) {
            loginTime = LocalDateTime.now();
        }
        if (lastAccessTime == null) {
            lastAccessTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateLastAccess() {
        this.lastAccessTime = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}