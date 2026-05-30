package com.thedal.thedal_app.session.dto;

import lombok.Data;

@Data
public class DeviceInfo {
    private String ipAddress;
    private String userAgent;
    private String deviceType;
    private String browserName;
    private String operatingSystem;
    private String locationCountry;
    private String locationCity;
    private String locationRegion;

    public DeviceInfo(String ipAddress, String userAgent) {
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        parseUserAgent(userAgent);
    }

    private void parseUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            this.deviceType = "Unknown";
            this.browserName = "Unknown";
            this.operatingSystem = "Unknown";
            return;
        }

        String lowerUserAgent = userAgent.toLowerCase();
        
        // Detect device type
        if (lowerUserAgent.contains("mobile") || lowerUserAgent.contains("android")) {
            this.deviceType = "Mobile";
        } else if (lowerUserAgent.contains("tablet") || lowerUserAgent.contains("ipad")) {
            this.deviceType = "Tablet";
        } else {
            this.deviceType = "Desktop";
        }

        // Detect browser
        if (lowerUserAgent.contains("chrome") && !lowerUserAgent.contains("edg")) {
            this.browserName = "Chrome";
        } else if (lowerUserAgent.contains("firefox")) {
            this.browserName = "Firefox";
        } else if (lowerUserAgent.contains("safari") && !lowerUserAgent.contains("chrome")) {
            this.browserName = "Safari";
        } else if (lowerUserAgent.contains("edg")) {
            this.browserName = "Edge";
        } else if (lowerUserAgent.contains("opera") || lowerUserAgent.contains("opr")) {
            this.browserName = "Opera";
        } else {
            this.browserName = "Unknown";
        }

        // Detect operating system
        if (lowerUserAgent.contains("windows")) {
            this.operatingSystem = "Windows";
        } else if (lowerUserAgent.contains("mac") && !lowerUserAgent.contains("iphone")) {
            this.operatingSystem = "macOS";
        } else if (lowerUserAgent.contains("linux")) {
            this.operatingSystem = "Linux";
        } else if (lowerUserAgent.contains("android")) {
            this.operatingSystem = "Android";
        } else if (lowerUserAgent.contains("iphone") || lowerUserAgent.contains("ipad")) {
            this.operatingSystem = "iOS";
        } else {
            this.operatingSystem = "Unknown";
        }
    }
}