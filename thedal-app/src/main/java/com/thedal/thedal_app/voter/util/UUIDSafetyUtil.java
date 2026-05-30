package com.thedal.thedal_app.voter.util;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for safe UUID operations during the migration period
 * This ensures backward compatibility while fixing the UUID issues
 */
public class UUIDSafetyUtil {
    
    private static final Logger log = LoggerFactory.getLogger(UUIDSafetyUtil.class);
    
    /**
     * Safely converts a string to UUID with proper error handling
     * @param uuidString The string to convert
     * @return UUID or null if conversion fails
     */
    public static UUID safeStringToUUID(String uuidString) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            return null;
        }
        
        try {
            return UUID.fromString(uuidString.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Failed to convert string to UUID: {}, error: {}", uuidString, e.getMessage());
            return null;
        }
    }
    
    /**
     * Safely converts UUID to string with null safety
     * @param uuid The UUID to convert
     * @return String representation or null
     */
    public static String safeUUIDToString(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }
    
    /**
     * Generates a new UUID with error handling
     * @return New UUID or null if generation fails
     */
    public static UUID safeGenerateUUID() {
        try {
            return UUID.randomUUID();
        } catch (Exception e) {
            log.error("Failed to generate UUID: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Compares two UUIDs safely handling nulls
     * @param uuid1 First UUID
     * @param uuid2 Second UUID  
     * @return true if equal (including both null), false otherwise
     */
    public static boolean safeUUIDEquals(UUID uuid1, UUID uuid2) {
        if (uuid1 == null && uuid2 == null) {
            return true;
        }
        if (uuid1 == null || uuid2 == null) {
            return false;
        }
        return uuid1.equals(uuid2);
    }
}
