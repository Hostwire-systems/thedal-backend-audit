package com.thedal.thedal_app.migration;

/**
 * Enumeration of different types of global migration jobs
 */
public enum GlobalMigrationJobType {
    
    /**
     * Complete migration of all data across all accounts
     */
    COMPLETE_GLOBAL,
    
    /**
     * Migration of specific accounts only
     */
    ACCOUNT_SPECIFIC,
    
    /**
     * Migration of voter data only
     */
    VOTER_ONLY,
    
    /**
     * Migration of all data except voters
     */
    COMPREHENSIVE_NON_VOTER,
    
    /**
     * Migration of everything including PartManager data
     */
    COMPLETE_EVERYTHING,
    
    /**
     * Migration of PartManager data only
     */
    PART_MANAGER_ONLY,
    
    /**
     * Migration of Section data only
     */
    SECTION_ONLY,
    
    /**
     * Migration of both PartManager and Section data
     */
    PART_MANAGER_AND_SECTION,
    
    /**
     * Migration of settings data only
     */
    SETTINGS_ONLY,
    
    /**
     * Migration of user data only
     */
    USER_ONLY,
    
    /**
     * Migration of universal data only
     */
    UNIVERSAL_ONLY,
    
    /**
     * Custom migration with specific modules
     */
    CUSTOM
}
