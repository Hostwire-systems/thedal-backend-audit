package com.thedal.thedal_app.merge;

/**
 * Enumerates mergeable voter data fields selectable in the UI.
 * Excludes metadata fields (id, accountId, electionId, timestamps, etc.).
 * List mirrors the voter create/edit form fields so "Select All" truly copies everything users maintain.
 */
public enum MergeField {
    // Contact & Identity
    MOBILE_NUMBER,
    WHATSAPP_NUMBER,
    EMAIL_ID,
    AADHAAR_NUMBER,
    PAN_NUMBER,
    MEMBERSHIP_NUMBER,

    // Personal Details
    DATE_OF_BIRTH,
    AGE,
    GENDER,
    PHOTO_URL,
    VIDEO_URL,
    STAR_NUMBER,

    // Geo & Address
    VOTER_LATITUDE,
    VOTER_LONGITUDE,
    PART_LATITUDE,
    PART_LONGITUDE,
    FULL_ADDRESS,
    PINCODE,

    // Part/Section identifiers
    BOOTH_NUMBER,
    PART_NUMBER,
    SECTION_NUMBER,
    SERIAL_NUMBER,
    PAGE_NUMBER,

    // House & Names
    HOUSE_NO_EN,
    HOUSE_NO_L1,
    HOUSE_NO_L2,
    VOTER_FNAME_EN,
    VOTER_LNAME_EN,
    VOTER_FNAME_L1,
    VOTER_LNAME_L1,
    VOTER_FNAME_L2,
    VOTER_LNAME_L2,

    // Relation details
    RLN_TYPE,
    RLN_FNAME_EN,
    RLN_LNAME_EN,
    RLN_FNAME_L1,
    RLN_LNAME_L1,
    RLN_FNAME_L2,
    RLN_LNAME_L2,

    // Section/Part display names
    SECTION_NAME_EN,
    SECTION_NAME_L1,
    SECTION_NAME_L2,
    PART_NAME_EN,
    PART_NAME_L1,
    PART_NAME_L2,

    // Key identifiers
    EPIC_NUMBER,

    // Religious & Caste
    RELIGION,
    CASTE,
    SUB_CASTE,
    CASTE_CATEGORY,

    // Political
    PARTY,
    PARTY_AFFILIATION,
    VOTER_CATEGORY,

    // Collections
    LANGUAGE,
    BENEFIT_SCHEMES,
    FEEDBACK,
    VOTER_HISTORY,

    // Relationships
    FAMILY_MAPPING,
    FRIENDS_MAPPING,

    // Notes
    REMARKS
}
