package com.thedal.thedal_app.auth.session;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_device_session")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDeviceSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId; // short identifier returned to client

    @Column(name = "jti", nullable = false, length = 64)
    private String jti;      // JWT ID for current access token

    @Column(name = "refresh_token_hash", length = 128)
    private String refreshTokenHash;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(name = "platform", length = 100)
    private String platform;

    @Column(name = "browser", length = 100)
    private String browser;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "password_version_at_issue")
    private Integer passwordVersionAtIssue;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (lastActiveAt == null) lastActiveAt = now;
    }
}
