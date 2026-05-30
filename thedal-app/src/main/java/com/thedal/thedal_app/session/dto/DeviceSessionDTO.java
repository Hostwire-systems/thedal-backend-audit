package com.thedal.thedal_app.session.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DeviceSessionDTO {

    @JsonProperty("session_id")
    private Long sessionId;

    @JsonProperty("device_type")
    private String deviceType;

    @JsonProperty("browser_name")
    private String browserName;

    @JsonProperty("operating_system")
    private String operatingSystem;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("location")
    private String location;

    @JsonProperty("login_time")
    private LocalDateTime loginTime;

    @JsonProperty("last_access_time")
    private LocalDateTime lastAccessTime;

    @JsonProperty("is_current_session")
    private Boolean isCurrentSession = false;

    public DeviceSessionDTO(Long sessionId, String deviceType, String browserName, 
                           String operatingSystem, String ipAddress, String locationCountry, 
                           String locationCity, LocalDateTime loginTime, LocalDateTime lastAccessTime) {
        this.sessionId = sessionId;
        this.deviceType = deviceType;
        this.browserName = browserName;
        this.operatingSystem = operatingSystem;
        this.ipAddress = ipAddress;
        this.location = buildLocationString(locationCountry, locationCity);
        this.loginTime = loginTime;
        this.lastAccessTime = lastAccessTime;
    }

    private String buildLocationString(String country, String city) {
        if (city != null && country != null) {
            return city + ", " + country;
        } else if (country != null) {
            return country;
        } else if (city != null) {
            return city;
        }
        return "Unknown Location";
    }
}