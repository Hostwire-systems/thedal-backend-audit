package com.thedal.thedal_app.auth.session;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SessionDto {
    private Long id;
    private String deviceId;
    private String deviceName;
    private String platform;
    private String browser;
    private String ipAddressMasked;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
    private boolean current;

    public static SessionDto from(UserDeviceSession s) {
        SessionDto dto = new SessionDto();
        dto.id = s.getId();
        dto.deviceId = s.getDeviceId();
        dto.deviceName = s.getDeviceName();
        dto.platform = s.getPlatform();
        dto.browser = s.getBrowser();
        dto.createdAt = s.getCreatedAt();
        dto.lastActiveAt = s.getLastActiveAt();
        dto.ipAddressMasked = maskIp(s.getIpAddress());
        return dto;
    }

    public SessionDto markCurrentIf(boolean condition) {
        this.current = condition;
        return this;
    }

    private static String maskIp(String ip) {
        if (ip == null) return null;
        if (ip.contains(".")) { // IPv4 simple mask
            String[] parts = ip.split("\\.");
            if (parts.length == 4) return parts[0] + "." + parts[1] + ".*.*";
        }
        if (ip.contains(":")) { // IPv6
            return ip.substring(0, Math.min(8, ip.length())) + "::****";
        }
        return ip;
    }
}
