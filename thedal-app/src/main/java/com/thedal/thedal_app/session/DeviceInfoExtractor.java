package com.thedal.thedal_app.session;

import org.springframework.stereotype.Component;

import com.thedal.thedal_app.session.dto.DeviceInfo;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DeviceInfoExtractor {

    /**
     * Extract device information from HTTP request
     */
    public DeviceInfo extractFromRequest(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        String ipAddress = getClientIpAddress(request);
        
        log.debug("Extracting device info - IP: {}, UserAgent: {}", ipAddress, userAgent);
        
        DeviceInfo deviceInfo = new DeviceInfo(ipAddress, userAgent);
        
        // TODO: Add geolocation lookup based on IP address
        // This could be integrated with services like MaxMind GeoIP, ipapi, etc.
        // For now, we'll leave location fields as null
        
        return deviceInfo;
    }

    /**
     * Get user agent from request headers
     */
    private String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "Unknown User Agent";
        }
        return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
    }

    /**
     * Get client IP address, handling various proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                if (isValidIP(ip)) {
                    log.debug("Found IP address from header {}: {}", header, ip);
                    return ip;
                }
            }
        }

        // Fallback to request.getRemoteAddr()
        String remoteAddr = request.getRemoteAddr();
        log.debug("Using remote address: {}", remoteAddr);
        return remoteAddr != null ? remoteAddr : "Unknown IP";
    }

    /**
     * Basic IP address validation
     */
    private boolean isValidIP(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        // Skip localhost and private ranges for proxy detection
        if ("127.0.0.1".equals(ip) || 
            "0:0:0:0:0:0:0:1".equals(ip) || 
            "::1".equals(ip) ||
            ip.startsWith("192.168.") ||
            ip.startsWith("10.") ||
            ip.startsWith("172.")) {
            return true; // These are valid but might indicate proxy usage
        }
        
        // Basic format check for IPv4
        if (ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
            return true;
        }
        
        // Basic format check for IPv6 (simplified)
        if (ip.contains(":")) {
            return true;
        }
        
        return false;
    }
}