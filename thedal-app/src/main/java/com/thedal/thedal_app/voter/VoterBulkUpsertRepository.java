package com.thedal.thedal_app.voter;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * High‑performance batch upsert for {@link VoterEntity}. All columns that exist on the
 * voter table are enumerated explicitly so we hit the DB only once per batch instead of one
 * insert / update per voter.
 *
 * ⚠️  The table is assumed to have a **composite unique key** on
 * <code>(account_id, election_id, epic_number)</code>. Adjust the ON DUPLICATE KEY/ON CONFLICT
 * clause if your schema differs.
 */
@Repository
@RequiredArgsConstructor
public class VoterBulkUpsertRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final String TABLE = "voter";

    /**
     * Every physical column on the voter table **except** the surrogate <code>id</code> (and any
     * dynamic columnX). If you add a new column later, drop it into this array and regenerate the
     * SQL constant below.
     */
    private static final String[] COLUMNS = {
        // identity / ownership
        "account_id", "election_id", "epic_number",
        // basic demographics
        "gender", "part_no", "serial_no",
        // house / voter names (3 language layers)
        "house_no_en", "house_no_l1", "house_no_l2",
        "voter_fname_en", "voter_lname_en", "voter_fname_l1", "voter_lname_l1", "voter_fname_l2", "voter_lname_l2",
        // relationship names
        "rln_type", "rln_fname_en", "rln_lname_en", "rln_fname_l1", "rln_lname_l1", "rln_fname_l2", "rln_lname_l2",
        // section / booth
        "section_no", "section_name_en", "section_name_l1", "section_name_l2", "booth_number",
        // address & part
        "full_address", "part_name_en", "part_name_l1", "part_name_l2", "pincode",
        // geo
        "part_lati", "part_long", "voter_lati", "voter_longi",
        // personal
        "age", "dob",
        // contact
        "mobile_no", "whatsapp_no", "e_mail",
        // state / district (3 language layers)
        "state_code", "state_name_en", "state_name_l1", "state_name_l2",
        "district_code", "district_name_en", "district_name_l1", "district_name_l2",
        // pc / ac (3 language layers)
        "pc_no", "pc_name_en", "pc_name_l1", "pc_name_l2",
        "ac_no", "ac_name_en", "ac_name_l1", "ac_name_l2",
        // urban / rural hierarchy
        "urban_no", "urban_name_en", "urban_name_l1", "urban_ward_no",
        "rur_district_union_no", "rur_district_union_name_en", "rur_district_union_name_l1", "rur_district_union_name_l2", "rur_district_union_ward_no",
        "pan_union_no", "pan_union_name_en", "pan_union_name_l1", "pan_union_name_l2", "pan_union_ward_no",
        "vill_pan_no", "vill_pan_name_en", "vill_pan_name_l1", "vill_pan_ward_no",
        // media
        "photo_url", "video_url",
        // misc voter meta
        "party_affiliation", "family_count", "has_voted", "voted_timestamp",
        "page_number", "remarks", "star_number",
        // KYC
        "aadhaar_number", "pan_number", "party_registration_number",
        "mobile_verified", "aadhaar_verified", "member_verified",
        // bookkeeping
        "created_time"
    };

    /** SQL generated once at class‑load. */
    private static final String INSERT_SQL = buildInsertSql();

    private static String buildInsertSql() {
        String cols = String.join(", ", COLUMNS);
        String placeHolders = Arrays.stream(COLUMNS).map(c -> "?").collect(Collectors.joining(", "));
        String updates = Arrays.stream(COLUMNS)
                               .filter(c -> !"created_time".equals(c))
                               .map(c -> c + " = VALUES(" + c + ")")
                               .collect(Collectors.joining(", "));
        return "INSERT INTO " + TABLE + " (" + cols + ") VALUES (" + placeHolders + ") " +
               "ON DUPLICATE KEY UPDATE " + updates;
    }

    /**
     * Executes one round‑trip batch upsert for the supplied voters.
     */
    public void upsertBatch(List<VoterEntity> voters) {
        jdbcTemplate.batchUpdate(INSERT_SQL, voters.stream()
                                                   .map(this::mapArgs)
                                                   .collect(Collectors.toList()));
    }

    private Object[] mapArgs(VoterEntity v) {
        return new Object[] {
            v.getAccountId(), v.getElectionId(), v.getEpicNumber(),
            v.getGender(), v.getPartNo(), v.getSerialNo(),
            v.getHouseNoEn(), v.getHouseNoL1(), v.getHouseNoL2(),
            v.getVoterFnameEn(), v.getVoterLnameEn(), v.getVoterFnameL1(), v.getVoterLnameL1(), v.getVoterFnameL2(), v.getVoterLnameL2(),
            v.getRlnType(), v.getRlnFnameEn(), v.getRlnLnameEn(), v.getRlnFnameL1(), v.getRlnLnameL1(), v.getRlnFnameL2(), v.getRlnLnameL2(),
            v.getSectionNo(), v.getSectionNameEn(), v.getSectionNameL1(), v.getSectionNameL2(), v.getBoothNumber(),
            v.getFullAddress(), v.getPartNameEn(), v.getPartNameL1(), v.getPartNameL2(), v.getPincode(),
            v.getPartLati(), v.getPartLong(), v.getVoterLati(), v.getVoterLongi(),
            v.getAge(), v.getDob(),
            v.getMobileNo(), v.getWhatsappNo(), v.getEMail(),
            v.getStateCode(), v.getStateNameEn(), v.getStateNameL1(), v.getStateNameL2(),
            v.getDistrictCode(), v.getDistrictNameEn(), v.getDistrictNameL1(), v.getDistrictNameL2(),
            v.getPcNo(), v.getPcNameEn(), v.getPcNameL1(), v.getPcNameL2(),
            v.getAcNo(), v.getAcNameEn(), v.getAcNameL1(), v.getAcNameL2(),
            v.getUrbanNo(), v.getUrbanNameEn(), v.getUrbanNameL1(), v.getUrbanWardNo(),
            v.getRurDistrictUnionNo(), v.getRurDistrictUnionNameEn(), v.getRurDistrictUnionNameL1(), v.getRurDistrictUnionNameL2(), v.getRurDistrictUnionWardNo(),
            v.getPanUnionNo(), v.getPanUnionNameEn(), v.getPanUnionNameL1(), v.getPanUnionNameL2(), v.getPanUnionWardNo(),
            v.getVillPanNo(), v.getVillPanNameEn(), v.getVillPanNameL1(), v.getVillPanWardNo(),
            v.getPhotoUrl(), v.getVideoUrl(),
            v.getPartyAffiliation(), v.getFamilyCount(), v.getHasVoted(), v.getVotedTimestamp(),
            v.getPageNumber(), v.getRemarks(), v.getStarNumber(),
            v.getAadhaarNumber(), v.getPanNumber(), v.getPartyRegistrationNumber(),
            v.getMobileVerified(), v.getAadhaarVerified(), v.getMemberVerified(),
            Timestamp.valueOf(v.getCreatedTime() != null ? v.getCreatedTime() : LocalDateTime.now())
        };
    }
}
