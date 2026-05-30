package com.thedal.thedal_app.voter;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thedal.thedal_app.voter.dto.BoothGenderStatsProjection;
import com.thedal.thedal_app.voter.dto.GenderStatsProjection;
import com.thedal.thedal_app.voter.dto.VoterExportProjection;
import com.thedal.thedal_app.voter.dto.VoterStatusDTO;

import jakarta.persistence.QueryHint;
import jakarta.transaction.Transactional;



@Repository
public interface VoterRepo extends JpaRepository<VoterEntity, Long>,
                                   JpaSpecificationExecutor<VoterEntity> {
	@Query("SELECT COUNT(v) > 0 FROM VoterEntity v WHERE v.electionId = :electionId AND v.accountId = :accountId")
    boolean existsByElectionIdAndAccountId(@Param("electionId") Long electionId, 
                                          @Param("accountId") Long accountId);
	Optional<VoterEntity> findByVoterIdAndElectionId(String voterId, Long electionId);

	Optional<VoterEntity> findByVoterIdAndAccountId(String voterId, Long accountId);
	
	List<VoterEntity>findByAccountId(Long accountId);	

	@Query("SELECT v FROM VoterEntity v " +
	       "LEFT JOIN FETCH v.caste " +
	       "LEFT JOIN FETCH v.subCaste " +
	       "LEFT JOIN FETCH v.religion " +
	       "LEFT JOIN FETCH v.party " +
	       "LEFT JOIN FETCH v.availability1 " +
	       "WHERE v.accountId = :accountId AND v.electionId = :electionId ORDER BY v.partNo, v.serialNo")
	Page<VoterEntity> findByAccountIdAndElectionId(@Param("accountId") Long accountId, @Param("electionId") Long electionId, Pageable pageable);

	long countByAccountIdAndElectionId(@Param("accountId") Long accountId, @Param("electionId") Long electionId);

	Page<VoterEntity> findAll(Pageable pageable);
//	@Query("SELECT COUNT(v) FROM VoterEntity v")
//    long count();
	long count();

	Optional<VoterEntity> findByAccountIdAndElectionIdAndVoterId(Long accountId, Long electionId, String voterId);

	Page<VoterEntity> findByAccountIdAndVoterIdAndElectionId(Long accountId, String voterId, Long electionId,
			Pageable pageable);

//	Page<VoterEntity> findByAccountIdAndElectionIdAndBoothNumber(Long accountId, Long electionId, Integer boothNumber,
//			Pageable pageable);
	Page<VoterEntity> findByAccountIdAndElectionIdAndBoothNumberIn(Long accountId, Long electionId, List<Integer> boothNumbers, Pageable pageable);

	Page<VoterEntity> findByElectionIdAndBoothNumberAndAccountId(Long electionId, Long boothNumber, Long accountId,
			Pageable pageable);

	Page<VoterEntity> findByElectionIdAndAccountId(Long electionId, Long accountId, Pageable pageable);

	Optional<VoterEntity> findByVoterIdAndElectionIdAndAccountId(String voterId, Long electionId, Long accountId);
	//Optional<VoterEntity> findByVoterIdAndElectionIdAndAccountId(String epicNumber, Long electionId, Long accountId);

	@Query("SELECT v FROM VoterEntity v WHERE LOWER(TRIM(v.voterId)) = LOWER(TRIM(:voterId)) " +
	           "AND v.electionId = :electionId AND v.accountId = :accountId")
	    boolean existsByVoterIdAndElectionIdAndAccountId(
	        @Param("voterId") String voterId,
	        @Param("electionId") Long electionId,
	        @Param("accountId") Long accountId);
	
//	@Query("SELECT v FROM VoterEntity v WHERE LOWER(TRIM(v.voterId)) = LOWER(TRIM(:voterId)) " +
//	           "AND v.electionId = :electionId AND v.accountId = :accountId")
//	    Optional<VoterEntity> findByVoterIdAndElectionIdAndAccountId(
//	        @Param("voterId") String voterId,
//	        @Param("electionId") Long electionId,
//	        @Param("accountId") Long accountId);
	
	
	//List<VoterEntity> findAllByVoterIdInAndElectionId(List<String> voterIds, Long electionId);
	List<VoterEntity> findAllByEpicNumberInAndElectionId(List<String> epicNumbers, Long electionId);	

	@Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND UPPER(v.epicNumber) IN :upperEpics")
	List<VoterEntity> findByAccountIdAndElectionIdAndEpicNumberInIgnoreCase(
		@Param("accountId") Long accountId,
		@Param("electionId") Long electionId,
		@Param("upperEpics") List<String> upperEpics);
	
	Optional<VoterEntity> findByVoterIdAndEpicNumberAndBoothNumberAndElectionId(String voterId, String epicNumber,
			Integer boothNumber, Long electionId);
	
	Optional<VoterEntity> findByAccountIdAndVoterIdAndEpicNumberAndBoothNumberAndElectionId(
		    Long accountId, String string, String epicNumber, Integer integer, Long electionId);
	Optional<VoterEntity> findByAccountIdAndVoterIdAndElectionId(Long accountId, String voterId, Long electionId);

	// Find voter by election, part number, serial number, and account ID for photo updates
	Optional<VoterEntity> findByElectionIdAndPartNoAndSerialNoAndAccountId(Long electionId, Integer partNo, Long serialNo, Long accountId);

	Optional<VoterEntity> findByAccountIdAndBoothNumberAndElectionId(Long accountId, Integer boothNumber,
			Long electionId);

	@Query("SELECT v FROM VoterEntity v WHERE LOWER(TRIM(v.epicNumber)) = LOWER(TRIM(:epicNumber)) " +
	           "AND v.electionId = :electionId AND v.accountId = :accountId")
	    Optional<VoterEntity> findByEpicNumberAndElectionIdAndAccountId(
	        @Param("epicNumber") String epicNumber,
	        @Param("accountId") Long accountId,
	        @Param("electionId") Long electionId);

	    @Query("SELECT COUNT(v) FROM VoterEntity v WHERE v.familyId = :familyId AND v.electionId = :electionId AND v.accountId = :accountId")
	    int countByFamilyIdAndElectionIdAndAccountId(
	        @Param("familyId") UUID familyId,
	        @Param("electionId") Long electionId,
	        @Param("accountId") Long accountId);

	    @Modifying
	    @Query("UPDATE VoterEntity v SET v.familyCount = :familyCount WHERE v.familyId = :familyId")
	    void updateFamilyCountForFamily(@Param("familyId") UUID familyId, @Param("familyCount") int familyCount);

	    @Modifying
	    @Query("UPDATE VoterEntity v SET v.familyId = :newFamilyId WHERE v.familyId = :oldFamilyId")
	    void updateFamilyIdForAllVoters(@Param("oldFamilyId") UUID oldFamilyId, @Param("newFamilyId") UUID newFamilyId);
	


	    // Debugging method to fetch all voters for an election and account
	    @Query("SELECT v FROM VoterEntity v WHERE v.electionId = :electionId AND v.accountId = :accountId")
	    List<VoterEntity> findAllByElectionIdAndAccountId(
	            @Param("electionId") Long electionId,
	            @Param("accountId") Long accountId);
	
	
	
	Optional<VoterEntity> findByAccountIdAndEpicNumberAndElectionId(Long accountId, String epicNumber, Long electionId);

	Optional<VoterEntity> findByEpicNumberAndElectionId(String epicNumber, Long electionId);

	List<VoterEntity> findByAccountIdAndElectionId(Long accountId, Long electionId);
	
	// Find voters without photos for bulk S3 upload
	List<VoterEntity> findByAccountIdAndElectionIdAndPhotoUrlIsNull(Long accountId, Long electionId);

	@EntityGraph(attributePaths = {
		"voterHistories", 
		"feedbackIssues", 
		"religion", 
		"caste", 
		"subCaste", 
		"casteCategory", 
		"party", 
		"availability1", 
		"languages",
		"voterBenefitSchemes",
		"voterBenefitSchemes.benefitScheme"
	})
	@Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId")
	List<VoterEntity> findByAccountIdAndElectionIdWithHistoriesAndFeedback(@Param("accountId") Long accountId, @Param("electionId") Long electionId);

//	@Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND LOWER(v.epicNumber) = LOWER(:epicNumber)")
//    Page<VoterEntity> findByAccountIdAndElectionIdAndEpicNumber(@Param("accountId") Long accountId,
//                                                               @Param("electionId") Long electionId,
//                                                               @Param("epicNumber") String epicNumber,
//                                                               Pageable pageable);
	@Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND UPPER(v.epicNumber) = UPPER(:epicNumber)")
    Page<VoterEntity> findByAccountIdAndElectionIdAndEpicNumberIgnoreCase(
        @Param("accountId") Long accountId, 
        @Param("electionId") Long electionId, 
        @Param("epicNumber") String epicNumber, 
        Pageable pageable);

	@Query("SELECT COUNT(v) > 0 FROM VoterEntity v WHERE v.subCaste.id = :subCasteId AND v.accountId = :accountId")
	boolean existsBySubCasteAndAccountId(@Param("subCasteId") Long subCasteId, @Param("accountId") Long accountId);

	List<VoterEntity> findAllByVoterIdIn(List<String> voterIds);

	Optional<VoterEntity> findByVoterId(String voterId);

	boolean existsByVoterId(String voterId);

	boolean existsByReligionIdAndAccountIdAndElectionId(Long religionId, Long accountId, Long electionId);

	boolean existsByCaste_IdAndAccountIdAndElectionId(Long casteId, Long accountId, Long electionId);
	//Page<VoterEntity> findByAccountIdAndElectionIdAndBoothNumberIn(Long accountId, Long electionId, List<Integer> boothNumbers, Pageable pageable);
	Page<VoterEntity> findByAccountIdAndVoterIdAndElectionIdAndBoothNumberIn(Long accountId, String voterId, Long electionId, List<Long> boothNumbers, Pageable pageable);

    Page<VoterEntity> findByAccountIdAndElectionIdAndEpicNumberAndBoothNumberIn(Long accountId, Long electionId, String epicNumber, List<Long> boothNumbers, Pageable pageable);
	
	Optional<VoterEntity> findByAvailabilityAndElectionId(String availability, Long electionId);

	Optional<VoterEntity> findBySchemeAndElectionId(String availability, Long electionId);




	@Modifying
	@Transactional
	@Query(value = """
	    INSERT INTO _voters (epic_number, election_id, account_id, has_voted, part_no, section_no,serial_no,  
	        house_no_en, house_no_l1,voter_fname_en, voter_lname_en, voter_fname_l1,voter_lname_l1, 
	        rln_type, rln_fname_en, rln_lname_en, rln_fname_l1, rln_lname_l1,
	         gender, section_name_en, section_name_l1, full_address, part_name_en, part_name_l1, pincode, part_lati, part_long,
	        age, dob, mobile_no, whatsapp_no, e_mail, voter_lati, voter_longi, state_code, state_name_en, state_name_l1, district_code, district_name_en, district_name_l1,
	        pc_no, pc_name_en, pc_name_l1, ac_no, ac_name_en, ac_name_l1, urban_no, urban_name_en, urban_name_l1, urban_ward_no, rur_district_union_no, rur_district_union_name_en, rur_district_union_name_l1, rur_district_union_ward_no,
	        pan_union_no, pan_union_name_en, pan_union_name_l1, pan_union_ward_no,
	        vill_pan_no, vill_pan_name_en, vill_pan_name_l1, vill_pan_ward_no, booth_number, voter_id,
	        house_no_l2, voter_fname_l2, voter_lname_l2, rln_fname_l2, rln_lname_l2, section_name_l2,
	        part_name_l2, state_name_l2, district_name_l2, pc_name_l2, ac_name_l2, rur_district_union_name_l2, pan_union_name_l2,
	        remarks, page_number, created_time, family_count
	        
	       
	                	
	    ) VALUES (:epicNumber, :electionId, :accountId, :hasVoted, :partNo, :sectionNo,:serialNo,  
	        :houseNoEn, :houseNoL1,:voterFnameEn, :voterLnameEn, :voterFnameL1,:voterLnameL1, 
	        :rlnType,  :rlnFnameEn,:rlnLnameEn, :rlnFnameL1, :rlnLnameL1, 
	        :gender, :sectionNameEn, :sectionNameL1, :fullAddress, :partNameEn, :partNameL1, :pincode, :partLati, :partLong,
	        :age, :dob, :mobileNo, :whatsappNo, :eMail, :voterLati, :voterLongi,  :stateCode, :stateNameEn, :stateNameL1, :districtCode, :districtNameEn, :districtNameL1,
	        :pcNo, :pcNameEn, :pcNameL1, :acNo, :acNameEn, :acNameL1, :urbanNo, :urbanNameEn, :urbanNameL1, :urbanWardNo, :rurDistrictUnionNo, :rurDistrictUnionNameEn, :rurDistrictUnionNameL1, :rurDistrictUnionWardNo,
	        :panUnionNo, :panUnionNameEn, :panUnionNameL1, :panUnionWardNo,
	        :villPanNo, :villPanNameEn, :villPanNameL1, :villPanWardNo, :boothNumber, :voter_id, 
	        :houseNoL2, :voterFnameL2, :voterLnameL2, :rlnFnameL2, :rlnLnameL2, :sectionNameL2, 
	        :partNameL2, :stateNameL2, :districtNameL2, :pcNameL2, :acNameL2, :rurDistrictUnionNameL2, :panUnionNameL2,
	        :remarks, :pageNumber, :createdTime, :familyCount
	       
	          	        
	    ) ON CONFLICT (epic_number, election_id, account_id) DO UPDATE 
	    SET 
	         part_no   = EXCLUDED.part_no,
	         section_no   = EXCLUDED.section_no,
	        serial_no   = EXCLUDED.serial_no,
	        house_no_en    = EXCLUDED.house_no_en,
	        house_no_l1    = EXCLUDED.house_no_l1,
	        voter_fname_en = EXCLUDED.voter_fname_en,
	        voter_lname_en = EXCLUDED.voter_lname_en,
            voter_fname_l1 = EXCLUDED.voter_fname_l1,
            voter_lname_l1 = EXCLUDED.voter_lname_l1,
            rln_type       = EXCLUDED.rln_type,
	        rln_fname_en   = EXCLUDED.rln_fname_en,	       
            rln_lname_en   = EXCLUDED.rln_lname_en,
            rln_fname_l1   = EXCLUDED.rln_fname_l1, 
            rln_lname_l1   = EXCLUDED.rln_lname_l1,        	        
	        gender         = EXCLUDED.gender,
	        section_name_en = EXCLUDED.section_name_en,
            section_name_l1 = EXCLUDED.section_name_l1,
            full_address = EXCLUDED.full_address,
            part_name_en = EXCLUDED.part_name_en,
            part_name_l1 = EXCLUDED.part_name_l1,
            pincode    = EXCLUDED.pincode,
            part_lati  = EXCLUDED.part_lati,
            part_long  = EXCLUDED.part_long,
            age = EXCLUDED.age,
            dob = EXCLUDED.dob,
            mobile_no = EXCLUDED.mobile_no,
            whatsapp_no = EXCLUDED.whatsapp_no,
            e_mail = EXCLUDED.e_mail,
            voter_lati = EXCLUDED.voter_lati,
            voter_longi = EXCLUDED.voter_longi,
            state_code = EXCLUDED.state_code,
        state_name_en = EXCLUDED.state_name_en,
        state_name_l1 = EXCLUDED.state_name_l1,
        district_code = EXCLUDED.district_code,
        district_name_en = EXCLUDED.district_name_en,
        district_name_l1 = EXCLUDED.district_name_l1,
        pc_no = EXCLUDED.pc_no,
        pc_name_en = EXCLUDED.pc_name_en,
        pc_name_l1 = EXCLUDED.pc_name_l1,
        ac_no = EXCLUDED.ac_no,
        ac_name_en = EXCLUDED.ac_name_en,
        ac_name_l1 = EXCLUDED.ac_name_l1,
        urban_no = EXCLUDED.urban_no,
        urban_name_en = EXCLUDED.urban_name_en,
        urban_name_l1 = EXCLUDED.urban_name_l1,
        urban_ward_no = EXCLUDED.urban_ward_no,
        rur_district_union_no = EXCLUDED.rur_district_union_no,
        rur_district_union_name_en = EXCLUDED.rur_district_union_name_en,
        rur_district_union_name_l1 = EXCLUDED.rur_district_union_name_l1,
        rur_district_union_ward_no = EXCLUDED.rur_district_union_ward_no,
        pan_union_no = EXCLUDED.pan_union_no,
        pan_union_name_en = EXCLUDED.pan_union_name_en,
        pan_union_name_l1 = EXCLUDED.pan_union_name_l1,
        pan_union_ward_no = EXCLUDED.pan_union_ward_no,
        vill_pan_no = EXCLUDED.vill_pan_no,
        vill_pan_name_en = EXCLUDED.vill_pan_name_en,
        vill_pan_name_l1 = EXCLUDED.vill_pan_name_l1,
        vill_pan_ward_no = EXCLUDED.vill_pan_ward_no,
        booth_number = EXCLUDED.booth_number,
        voter_id = EXCLUDED.voter_id,
        house_no_l2 = EXCLUDED.house_no_l2,
        voter_fname_l2 = EXCLUDED.voter_fname_l2,
        voter_lname_l2 = EXCLUDED.voter_lname_l2,
        rln_fname_l2 = EXCLUDED.rln_fname_l2,
        rln_lname_l2 = EXCLUDED.rln_lname_l2,
        section_name_l2	= EXCLUDED.section_name_l2,
        part_name_l2 = EXCLUDED.part_name_l2,
        state_name_l2 = EXCLUDED.state_name_l2,
        district_name_l2 = EXCLUDED.district_name_l2,
        pc_name_l2  = EXCLUDED.pc_name_l2, 
        ac_name_l2  = EXCLUDED.ac_name_l2, 
        rur_district_union_name_l2 = EXCLUDED.rur_district_union_name_l2,
        pan_union_name_l2 = EXCLUDED.pan_union_name_l2,
        remarks = EXCLUDED.remarks,
        page_number = EXCLUDED.page_number,
        created_time = EXCLUDED.created_time,
        family_count = EXCLUDED.family_count
              
            		        
	    """, nativeQuery = true)
	void upsertVoter(
			@Param("epicNumber") String epicNumber,
		    @Param("electionId") Long electionId,
		    @Param("accountId") Long accountId,
		    @Param("hasVoted") Boolean hasVoted,
		    @Param("partNo") Integer partNo,
		    @Param("sectionNo") Integer sectionNo,
		    @Param("serialNo") Long serialNo,
		    @Param("houseNoEn") String houseNoEn,
		    @Param("houseNoL1") String houseNoL1,
		    @Param("voterFnameEn") String voterFnameEn,
		    @Param("voterLnameEn") String voterLnameEn,
		    @Param("voterFnameL1") String voterFnameL1,
		    @Param("voterLnameL1") String voterLnameL1,
		    @Param("rlnType") String rlnType,
		    @Param("rlnFnameEn") String rlnFnameEn,		    
		    @Param("rlnLnameEn") String rlnLnameEn,
		    @Param("rlnFnameL1") String rlnFnameL1,	
		    @Param("rlnLnameL1") String rlnLnameL1,		    
		    @Param("gender") String gender,
		    @Param("sectionNameEn") String sectionNameEn,
	        @Param("sectionNameL1") String sectionNameL1,
	        @Param("fullAddress") String fullAddress,
	        @Param("partNameEn") String partNameEn,
	        @Param("partNameL1") String partNameL1,
	        @Param("pincode") String pincode,
	        @Param("partLati") Double partLati,
	        @Param("partLong") Double partLong,
	        @Param("age") Integer age,
	        @Param("dob") LocalDate dob,
	        @Param("mobileNo") String mobileNo,
	        @Param("whatsappNo") String whatsappNo,
	        @Param("eMail") String eMail,
	        @Param("voterLati") Double voterLati,
	        @Param("voterLongi") Double voterLongi,
	        @Param("stateCode") String stateCode,
	        @Param("stateNameEn") String stateNameEn,
	        @Param("stateNameL1") String stateNameL1,
	        @Param("districtCode") String districtCode,
	        @Param("districtNameEn") String districtNameEn,
	        @Param("districtNameL1") String districtNameL1,
	        @Param("pcNo") String pcNo,
	        @Param("pcNameEn") String pcNameEn,
	        @Param("pcNameL1") String pcNameL1,
	        @Param("acNo") String acNo,
	        @Param("acNameEn") String acNameEn,
	        @Param("acNameL1") String acNameL1,
	        @Param("urbanNo") String urbanNo,
	        @Param("urbanNameEn") String urbanNameEn,
	        @Param("urbanNameL1") String urbanNameL1,
	        @Param("urbanWardNo") Integer urbanWardNo,
	        @Param("rurDistrictUnionNo") String rurDistrictUnionNo,
	        @Param("rurDistrictUnionNameEn") String rurDistrictUnionNameEn,
	        @Param("rurDistrictUnionNameL1") String rurDistrictUnionNameL1,
	        @Param("rurDistrictUnionWardNo") String rurDistrictUnionWardNo,
	        @Param("panUnionNo") String panUnionNo,
	        @Param("panUnionNameEn") String panUnionNameEn,
	        @Param("panUnionNameL1") String panUnionNameL1,
	        @Param("panUnionWardNo") String panUnionWardNo,
	        @Param("villPanNo") String villPanNo,
	        @Param("villPanNameEn") String villPanNameEn,
	        @Param("villPanNameL1") String villPanNameL1,
	        @Param("villPanWardNo") String villPanWardNo,
	        @Param("boothNumber") Integer boothNumber,
	        @Param("voter_id") String voterId,
	        @Param("houseNoL2") String houseNoL2,
	        @Param("voterFnameL2") String voterFnameL2,
	        @Param("voterLnameL2") String voterLnameL2,
	        @Param("rlnFnameL2") String rlnFnameL2,
	        @Param("rlnLnameL2") String rlnLnameL2,
	        @Param("sectionNameL2") String sectionNameL2,
	        @Param("partNameL2") String partNameL2,
	        @Param("stateNameL2") String stateNameL2,
	        @Param("districtNameL2") String districtNameL2,
	        @Param("pcNameL2") String pcNameL2,
	        @Param("acNameL2") String acNameL2,
	        @Param("rurDistrictUnionNameL2") String rurDistrictUnionNameL2,
	        @Param("panUnionNameL2") String panUnionNameL2,
	        @Param("remarks") String remarks,
	        @Param("pageNumber") Integer pageNumber,
	        @Param("createdTime") LocalDateTime createdTime,
	        @Param("familyCount") Integer familyCount
	        
	        
			   
	);

	Page<VoterEntity> findByAccountIdAndElectionIdAndFamilyId(Long accountId, Long electionId, UUID familyId, Pageable pageable);

	// Method to fetch all family members by family IDs (used for corrected family grouping logic)
	List<VoterEntity> findByAccountIdAndElectionIdAndFamilyIdIn(Long accountId, Long electionId, List<UUID> familyIds);

	// Optimized method to get all voters with families (no pagination overhead)
	List<VoterEntity> findByAccountIdAndElectionIdAndFamilyIdIsNotNull(Long accountId, Long electionId);

	// Optimized method to get all voters without families (no family voters)
	List<VoterEntity> findByAccountIdAndElectionIdAndFamilyIdIsNull(Long accountId, Long electionId);

	// Find voter by account, election and EPIC number for family ID generation
	Optional<VoterEntity> findByAccountIdAndElectionIdAndEpicNumber(Long accountId, Long electionId, String epicNumber);

	// FAST & SIMPLE: Get all voters with families filtered by part numbers  
	@Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND v.familyId IS NOT NULL AND v.partNo IN :partNumbers ORDER BY v.familyId, v.voterId")
	List<VoterEntity> findFamilyVotersByParts(
	        @Param("accountId") Long accountId, 
	        @Param("electionId") Long electionId, 
	        @Param("partNumbers") List<Integer> partNumbers);

//	// SUPER FAST: Get family summary with first member only (for initial load)
//	@Query(value = """
//	    SELECT DISTINCT ON (v.family_id) 
//	           v.family_id, 
//	           COUNT(*) OVER (PARTITION BY v.family_id) as member_count,
//	           v.voter_fname_en as first_member_name,
//	           v.epic_number as first_member_epic,
//	           v.age as first_member_age,
//	           v.gender as first_member_gender,
//	           v.part_no as first_member_part
//	    FROM _voters v 
//	    WHERE v.account_id = :accountId 
//	      AND v.election_id = :electionId 
//	      AND v.family_id IS NOT NULL
//	      AND v.part_no IN (:partNumbers)
//	    ORDER BY v.family_id, v.voter_id
//	    """, nativeQuery = true)
//	List<Object[]> findFamilySummaryByParts(
//	        @Param("accountId") Long accountId, 
//	        @Param("electionId") Long electionId, 
//	        @Param("partNumbers") List<Integer> partNumbers);
//
//	// SUPER FAST: Get family summary without part filter (when all parts requested)
//	@Query(value = """
//	    SELECT DISTINCT ON (v.family_id) 
//	           v.family_id, 
//	           COUNT(*) OVER (PARTITION BY v.family_id) as member_count,
//	           v.voter_fname_en as first_member_name,
//	           v.epic_number as first_member_epic,
//	           v.age as first_member_age,
//	           v.gender as first_member_gender,
//	           v.part_no as first_member_part
//	    FROM _voters v 
//	    WHERE v.account_id = :accountId 
//	      AND v.election_id = :electionId 
//	      AND v.family_id IS NOT NULL
//	    ORDER BY v.family_id, v.voter_id
//	    """, nativeQuery = true)
//	List<Object[]> findFamilySummaryAll(
//	        @Param("accountId") Long accountId, 
//	        @Param("electionId") Long electionId);
	@Query(value = """
	        SELECT DISTINCT ON (v.family_id) 
	               v.family_id, 
	               v.family_count as member_count,
	               v.voter_fname_en as first_member_name,
	               v.epic_number as first_member_epic,
	               v.age as first_member_age,
	               v.gender as first_member_gender,
	               v.part_no as first_member_part
	        FROM _voters v 
	        WHERE v.account_id = :accountId 
	          AND v.election_id = :electionId 
	          AND v.family_id IS NOT NULL
	        ORDER BY v.family_id, v.age DESC NULLS LAST, v.voter_id
	        """, nativeQuery = true)
	    List<Object[]> findFamilySummaryAll(
	            @Param("accountId") Long accountId, 
	            @Param("electionId") Long electionId);

			// Eldest anchoring: representative (eldest) chosen globally per family, then part / name filters applied AFTER selection.
			// Part filter now excludes entire family if eldest's part not in list; name filter does not influence grouping/order.
			@Query(value = """
					WITH reps AS (
							SELECT DISTINCT ON (v.family_id)
										 v.family_id,
										 v.family_count AS member_count,
										 v.voter_fname_en AS first_member_name,
										 v.epic_number AS first_member_epic,
										 v.age AS first_member_age,
										 v.gender AS first_member_gender,
										 v.part_no AS first_member_part
							FROM _voters v
							WHERE v.account_id = :accountId
								AND v.election_id = :electionId
								AND v.family_id IS NOT NULL
							ORDER BY v.family_id, v.age DESC NULLS LAST, v.voter_id
					)
					SELECT * FROM reps
					WHERE first_member_part IN (:partNumbers)
					ORDER BY family_id
					""", nativeQuery = true)
			List<Object[]> findFamilySummaryByParts(
							@Param("accountId") Long accountId,
							@Param("electionId") Long electionId,
							@Param("partNumbers") List<Integer> partNumbers);

			@Query(value = """
					WITH reps AS (
							SELECT DISTINCT ON (v.family_id)
										 v.family_id,
										 v.family_count AS member_count,
										 v.voter_fname_en AS first_member_name,
										 v.epic_number AS first_member_epic,
										 v.age AS first_member_age,
										 v.gender AS first_member_gender,
										 v.part_no AS first_member_part
							FROM _voters v
							WHERE v.account_id = :accountId
								AND v.election_id = :electionId
								AND v.family_id IS NOT NULL
							ORDER BY v.family_id, v.age DESC NULLS LAST, v.voter_id
					)
					SELECT * FROM reps
					WHERE (:partNumbers IS NULL OR first_member_part IN (:partNumbers))
						AND (:nameFilter IS NULL OR LOWER(first_member_name) LIKE :nameFilter)
					ORDER BY family_id
					""", nativeQuery = true)
			List<Object[]> findFamilySummaryByName(
							@Param("accountId") Long accountId,
							@Param("electionId") Long electionId,
							@Param("partNumbers") List<Integer> partNumbers,
							@Param("nameFilter") String nameFilter);

	// FAST: Get all members of a specific family
	@Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND v.familyId = :familyId ORDER BY v.isFamilyHead DESC NULLS LAST, v.age DESC NULLS LAST, v.voterId")
	List<VoterEntity> findFamilyMembers(
	        @Param("accountId") Long accountId, 
	        @Param("electionId") Long electionId, 
	        @Param("familyId") UUID familyId);

	// Fast family members with sorting - using JPA query to avoid column name mapping issues
	@Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND v.familyId = :familyId ORDER BY v.isFamilyHead DESC NULLS LAST, v.age DESC NULLS LAST, v.voterId")
	List<VoterEntity> findFamilyMembers(
	        @Param("accountId") Long accountId, 
	        @Param("electionId") Long electionId, 
	        @Param("familyId") UUID familyId,
	        Sort sort);

	// Paginated family members with sorting
	@Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND v.familyId = :familyId")
	Page<VoterEntity> findFamilyMembers(
	        @Param("accountId") Long accountId, 
	        @Param("electionId") Long electionId, 
	        @Param("familyId") UUID familyId,
	        Pageable pageable);

	// Fetch all voters for a given part number (used for part-wide family export)
	@Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND v.partNo = :partNo ORDER BY v.familyId, v.serialNo")
	List<VoterEntity> findByAccountIdAndElectionIdAndPartNo(@Param("accountId") Long accountId,
	                                                       @Param("electionId") Long electionId,
	                                                       @Param("partNo") Integer partNo);

	// Count voters for a given part number (used for size validation before export)
	@Query("SELECT COUNT(v) FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND v.partNo = :partNo")
	long countByAccountIdAndElectionIdAndPartNo(@Param("accountId") Long accountId,
	                                          @Param("electionId") Long electionId,
	                                          @Param("partNo") Integer partNo);

	//boolean existsByBenefitSchemes_Id(Long benefitSchemeId); 
	boolean existsBySchemeAndElectionId(String scheme, Long electionId);
	
	// Check if a family exists in the election
	boolean existsByFamilyIdAndElectionIdAndAccountId(UUID familyId, Long electionId, Long accountId);
	
	// Get family details by family IDs for family captain assignment (family head only)
	@Query("""
	       SELECT DISTINCT v.familyId, v.familySequenceNumber, 
	              COALESCE(v.voterFnameEn, '') || ' ' || COALESCE(v.voterLnameEn, ''), 
	              v.epicNumber, v.familyCount, v.partNo
	       FROM VoterEntity v 
	       WHERE v.accountId = :accountId 
	         AND v.electionId = :electionId 
	         AND v.familyId IN :familyIds
	         AND v.familyId IN (
	             SELECT DISTINCT subV.familyId 
	             FROM VoterEntity subV 
	             WHERE subV.accountId = :accountId 
	               AND subV.electionId = :electionId 
	               AND subV.familyId = v.familyId
	               AND (subV.isFamilyHead = true 
	                    OR (subV.isFamilyHead IS NULL AND subV.familySequenceNumber IS NOT NULL)
	                    OR (NOT EXISTS (
	                        SELECT 1 FROM VoterEntity checkV 
	                        WHERE checkV.familyId = subV.familyId 
	                          AND checkV.accountId = :accountId 
	                          AND checkV.electionId = :electionId 
	                          AND checkV.isFamilyHead = true
	                    ) AND subV.age = (
	                        SELECT MAX(maxV.age) 
	                        FROM VoterEntity maxV 
	                        WHERE maxV.familyId = subV.familyId 
	                          AND maxV.accountId = :accountId 
	                          AND maxV.electionId = :electionId
	                    ))
	               )
	               AND subV.id = v.id
	         )
	       ORDER BY v.familySequenceNumber, v.familyId
	       """)
	List<Object[]> findFamilyDetailsByIds(@Param("accountId") Long accountId,
	                                     @Param("electionId") Long electionId,
	                                     @Param("familyIds") List<UUID> familyIds);

	// Alternative query for family head identification using eldest member as fallback
	@Query("""
	       SELECT DISTINCT v.familyId, v.familySequenceNumber, 
	              COALESCE(v.voterFnameEn, '') || ' ' || COALESCE(v.voterLnameEn, ''), 
	              v.epicNumber, v.familyCount, v.partNo
	       FROM VoterEntity v 
	       WHERE v.accountId = :accountId 
	         AND v.electionId = :electionId 
	         AND v.familyId IN :familyIds
	         AND v.age = (
	             SELECT MAX(maxV.age) 
	             FROM VoterEntity maxV 
	             WHERE maxV.familyId = v.familyId 
	               AND maxV.accountId = :accountId 
	               AND maxV.electionId = :electionId
	         )
	       ORDER BY v.familySequenceNumber, v.familyId
	       """)
	List<Object[]> findFamilyDetailsByIdsWithoutFamilyHeadFilter(@Param("accountId") Long accountId,
	                                     @Param("electionId") Long electionId,
	                                     @Param("familyIds") List<UUID> familyIds);

	//Optional<VoterEntity> findByEpicNumberAndElectionIdAndAccountId(String epicNumber, String electionId, Long accountId);
	 
//	@Modifying
//    @Query("DELETE FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId")
//    int deleteByAccountIdAndElectionId(@Param("accountId") Long accountId, @Param("electionId") Long electionId);
	
	@Query("SELECT v FROM VoterEntity v WHERE LOWER(v.voterFnameEn) = LOWER(:voterFnameEn) AND v.accountId = :accountId AND v.electionId = :electionId")
    Page<VoterEntity> findByAccountIdAndElectionIdAndVoterFnameEnIgnoreCase(@Param("accountId") Long accountId,
                                                                           @Param("electionId") Long electionId,
                                                                           @Param("voterFnameEn") String voterFnameEn,
                                                                           Pageable pageable);

	
	@Query("SELECT v FROM VoterEntity v WHERE LOWER(v.voterFnameEn) IN :nameList AND v.accountId = :accountId AND v.electionId = :electionId")
    Page<VoterEntity> findByAccountIdAndElectionIdAndVoterFnameEnInIgnoreCase(@Param("accountId") Long accountId,
                                                                              @Param("electionId") Long electionId,
                                                                              @Param("nameList") List<String> nameList,
                                                                              Pageable pageable);

	 @Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND v.boothNumber IN :boothNumbers AND LOWER(v.voterFnameEn) = LOWER(:voterFnameEn)")
	    Page<VoterEntity> findByAccountIdAndElectionIdAndBoothNumberInAndVoterFnameEnIgnoreCase(@Param("accountId") Long accountId,
	                                                                                           @Param("electionId") Long electionId,
	                                                                                           @Param("boothNumbers") List<Integer> boothNumbers,
	                                                                                           @Param("voterFnameEn") String voterFnameEn,
	                                                                                           Pageable pageable);

	
	
	    //long countByElectionIdAndAccountId(Long electionId, Long accountId);

	    @Query("SELECT v FROM VoterEntity v WHERE v.electionId = :electionId AND v.accountId = :accountId ORDER BY v.voterId ASC")
	    List<VoterEntity> findByElectionIdAndAccountIdLimited(
	        @Param("electionId") Long electionId, 
	        @Param("accountId") Long accountId, 
	        Pageable pageable
	    );
	    
	    @Modifying
	    @Query("DELETE FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId")
	    int deleteByAccountIdAndElectionId(@Param("accountId") Long accountId, @Param("electionId") Long electionId);

//	    @Modifying
//	    @Query("DELETE FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND v.epicNumber IN :epicNumbers")
//	    int deleteByAccountIdAndElectionIdAndEpicNumbers(@Param("accountId") Long accountId, @Param("electionId") Long electionId, @Param("epicNumbers") List<String> epicNumbers);
	    @Modifying
	    @Query("DELETE FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND v.epicNumber IN :epicNumbers")
	    int deleteByAccountIdAndElectionIdAndEpicNumbers(@Param("accountId") Long accountId, 
	                                                    @Param("electionId") Long electionId, 
	                                                    @Param("epicNumbers") List<String> epicNumbers);
	    
	    @Query("SELECT COUNT(v) FROM VoterEntity v WHERE v.electionId = :electionId AND v.accountId = :accountId")
	    int countByElectionIdAndAccountId(@Param("electionId") Long electionId, @Param("accountId") Long accountId);

		//new added for avoiding server overload
		@Query("SELECT v FROM VoterEntity v WHERE v.electionId = :electionId AND v.accountId = :accountId")
		Page<VoterEntity> findPageByElectionIdAndAccountId(@Param("electionId") Long electionId,
														   @Param("accountId") Long accountId,
														   Pageable pageable);
		///////////////////////////////////////

	    @Query("SELECT v FROM VoterEntity v WHERE v.electionId = :electionId AND v.accountId = :accountId")
	    Page<VoterEntity> findByElectionIdAndAccountIdWithLimit(@Param("electionId") Long electionId,
	                                                             @Param("accountId") Long accountId,
	                                                             Pageable pageable);

			@Query("SELECT v FROM VoterEntity v WHERE v.electionId = :electionId AND v.accountId = :accountId")
			List<VoterEntity> findByElectionIdAndAccountId(@Param("electionId") Long electionId,
															@Param("accountId") Long accountId);

		boolean existsByAvailabilityAndElectionId(String description, Long electionId);


//////////////////////////////////////
			
		Page<VoterEntity> findByAccountIdAndElectionIdAndPartyPartyNameIgnoreCase(Long accountId, Long electionId, String partyName, Pageable pageable);
	    Page<VoterEntity> findByAccountIdAndElectionIdAndReligionReligionNameIgnoreCase(Long accountId, Long electionId, String religionName, Pageable pageable);
	    Page<VoterEntity> findByAccountIdAndElectionIdAndAge(Long accountId, Long electionId, Integer age, Pageable pageable);
	    Page<VoterEntity> findByAccountIdAndElectionIdAndAgeBetween(Long accountId, Long electionId, Integer minAge, Integer maxAge, Pageable pageable);
	    Page<VoterEntity> findByAccountIdAndElectionIdAndGenderIgnoreCase(Long accountId, Long electionId, String gender, Pageable pageable);

		// Combined method for volunteer case with all filters
    
	/////////////////////////////////////////////////
//	    @Query("SELECT " +
//	           "v.boothNumber as boothNumber, " +
//	           "SUM(CASE WHEN v.gender = 'male' THEN 1 ELSE 0 END) as maleCount, " +
//	           "SUM(CASE WHEN v.gender = 'female' THEN 1 ELSE 0 END) as femaleCount, " +
//	           "SUM(CASE WHEN v.gender = 'other' THEN 1 ELSE 0 END) as otherCount, " +
//	           "COUNT(*) as totalCount " +
//	           "FROM VoterEntity v " +
//	           "WHERE v.accountId = :accountId AND v.electionId = :electionId " +
//	           "AND (:boothNumbers IS NULL OR v.boothNumber IN :boothNumbers) " +
//	           "GROUP BY v.boothNumber")
//	    List<BoothGenderStatsProjection> getBoothGenderStats(Long accountId, Long electionId, List<Integer> boothNumbers);
	    @Query("SELECT " +
	            "v.boothNumber as boothNumber, " +
	            "SUM(CASE WHEN LOWER(TRIM(v.gender)) IN ('male', 'm') THEN 1 ELSE 0 END) as maleCount, " +
	            "SUM(CASE WHEN LOWER(TRIM(v.gender)) IN ('female', 'f') THEN 1 ELSE 0 END) as femaleCount, " +
	            "SUM(CASE WHEN LOWER(TRIM(v.gender)) NOT IN ('male', 'm', 'female', 'f') AND v.gender IS NOT NULL THEN 1 ELSE 0 END) as otherCount, " +
	            "COUNT(*) as totalCount " +
	            "FROM VoterEntity v " +
	            "WHERE v.accountId = :accountId " +
	            "AND v.electionId = :electionId " +
	            "GROUP BY v.boothNumber " +
	            "ORDER BY v.boothNumber")
	     List<BoothGenderStatsProjection> getAllBoothGenderStats(@Param("accountId") Long accountId, 
	                                                             @Param("electionId") Long electionId);
	     
	    @Query("SELECT " +
	            "v.boothNumber as boothNumber, " +
	            "SUM(CASE WHEN LOWER(TRIM(v.gender)) IN ('male', 'm') THEN 1 ELSE 0 END) as maleCount, " +
	            "SUM(CASE WHEN LOWER(TRIM(v.gender)) IN ('female', 'f') THEN 1 ELSE 0 END) as femaleCount, " +
	            "SUM(CASE WHEN LOWER(TRIM(v.gender)) NOT IN ('male', 'm', 'female', 'f') AND v.gender IS NOT NULL THEN 1 ELSE 0 END) as otherCount, " +
	            "COUNT(*) as totalCount " +
	            "FROM VoterEntity v " +
	            "WHERE v.accountId = :accountId " +
	            "AND v.electionId = :electionId " +
	            "AND v.boothNumber IN :boothNumbers " +  
	            "GROUP BY v.boothNumber " +
	            "ORDER BY v.boothNumber")
	     List<BoothGenderStatsProjection> getBoothGenderStats(@Param("accountId") Long accountId, 
	                                                          @Param("electionId") Long electionId, 
	                                                          @Param("boothNumbers") List<Integer> boothNumbers);
	    
//////////////////////////////////////////

		boolean existsByEpicNumberAndElectionIdAndAccountId(String epicNumber, Long electionId, Long accountId);
		
		// Add new query methods for filtering
	    @Query("SELECT v FROM VoterEntity v WHERE v.electionId = :electionId AND v.accountId = :accountId " +
	           "AND (:partNo IS NULL OR v.partNo = :partNo) " +
	           "AND (:gender IS NULL OR LOWER(v.gender) = LOWER(:gender)) " +
	           "AND (:ageMin IS NULL OR v.age >= :ageMin) " +
	           "AND (:ageMax IS NULL OR v.age <= :ageMax)")
	    Page<VoterEntity> findFilteredVotersWithLimit(@Param("electionId") Long electionId,
	                                                  @Param("accountId") Long accountId,
	                                                  @Param("partNo") Integer partNo,
	                                                  @Param("gender") String gender,
	                                                  @Param("ageMin") Integer ageMin,
	                                                  @Param("ageMax") Integer ageMax,
	                                                  Pageable pageable);

	    @Query("SELECT v FROM VoterEntity v WHERE v.electionId = :electionId AND v.accountId = :accountId " +
	           "AND (:partNo IS NULL OR v.partNo = :partNo) " +
	           "AND (:gender IS NULL OR LOWER(v.gender) = LOWER(:gender)) " +
	           "AND (:ageMin IS NULL OR v.age >= :ageMin) " +
	           "AND (:ageMax IS NULL OR v.age <= :ageMax)")
	    List<VoterEntity> findFilteredVoters(@Param("electionId") Long electionId,
	                                        @Param("accountId") Long accountId,
	                                        @Param("partNo") Integer partNo,
	                                        @Param("gender") String gender,
	                                        @Param("ageMin") Integer ageMin,
	                                        @Param("ageMax") Integer ageMax);

	    boolean existsByIdAndAccountId(Long electionId, Long accountId);

		Page<VoterEntity> findAll(Specification<VoterEntity> spec, Pageable pageable);

		List<VoterEntity> findAll(Specification<VoterEntity> spec); 
///////////////////////////////////////
		
		@Query("SELECT v FROM VoterEntity v " +
		           "LEFT JOIN v.availability1 a " +
		           "WHERE v.accountId = :accountId " +
		           "AND v.electionId = :electionId " +
		           "AND v.boothNumber IN :boothNumbers " +
		           "AND (:voterFnameEn IS NULL OR LOWER(v.voterFnameEn) = LOWER(:voterFnameEn)) " +
		           "AND (:partyName IS NULL OR (v.party IS NOT NULL AND v.party.partyName = :partyName)) " +
		           "AND (:religionName IS NULL OR (v.religion IS NOT NULL AND v.religion.religionName = :religionName)) " +
		           "AND (:age IS NULL OR v.age = :age) " +
		           "AND (:genders IS NULL OR LOWER(v.gender) IN :genders) " +
		           "AND (:hasDob IS NULL OR (:hasDob = true AND v.dob IS NOT NULL) OR (:hasDob = false AND v.dob IS NULL)) " +
		           "AND (:starNumber IS NULL OR v.starNumber = :starNumber) " +
		           "AND (:description IS NULL OR (a IS NOT NULL AND a.description = :description))")
		    Page<VoterEntity> findByAccountIdAndElectionIdAndBoothNumberInAndFilters(
		        @Param("accountId") Long accountId,
		        @Param("electionId") Long electionId,
		        @Param("boothNumbers") List<Integer> boothNumbers,
		        @Param("voterFnameEn") String voterFnameEn,
		        @Param("partyName") String partyName,
		        @Param("religionName") String religionName,
		        @Param("age") Integer age,
		        @Param("gender") String gender,
		        Pageable pageable);	
	// Methods for PDF processing service
	boolean existsByEpicNumberAndAccountIdAndElectionId(String epicNumber, Long accountId, Long electionId);
	
	VoterEntity findByEpicNumberAndAccountIdAndElectionId(String epicNumber, Long accountId, Long electionId);
	
	@Query("SELECT v FROM VoterEntity v WHERE LOWER(v.voterLnameEn) IN :nameList AND v.accountId = :accountId AND v.electionId = :electionId")
	Page<VoterEntity> findByAccountIdAndElectionIdAndVoterLnameEnInIgnoreCase(@Param("accountId") Long accountId,
			@Param("electionId") Long electionId,
			@Param("nameList") List<String> nameList,
			Pageable pageable);
		     
			boolean existsByAadhaarNumberAndElectionId(String aadhaarNumber, Long electionId);
			
			boolean existsByAadhaarNumberAndElectionIdAndEpicNumberNot(String aadhaarNumber, Long electionId, String epicNumber);
		     	     
 Optional<VoterEntity> findByAadhaarNumberAndElectionIdAndAccountId(String aadhaarNumber, Long electionId, Long accountId);
 
 List<VoterEntity> findByAccountIdAndElectionIdAndEpicNumberIn(Long accountId, Long electionId,
		List<String> cleanedEpicNumbers);

 
 @Query("SELECT v FROM VoterEntity v WHERE v.electionId = :electionId AND v.accountId = :accountId " +
         "AND (v.createdTime >= :cutoffDate OR v.modifiedTime >= :cutoffDate)")
  Page<VoterEntity> findByElectionIdAndAccountIdAndCreatedOrModifiedAfter(
          @Param("electionId") Long electionId,
          @Param("accountId") Long accountId,
          @Param("cutoffDate") LocalDateTime cutoffDate,
          Pageable pageable);

//	@Modifying
//    @Transactional
//    @Query("UPDATE VoterEntity v SET v.familyId = :newFamilyId WHERE v.familyId = :oldFamilyId")
//    void updateFamilyIdForAllVoters(@Param("oldFamilyId") UUID oldFamilyId, @Param("newFamilyId") UUID newFamilyId); 

		boolean existsByPartyId(Long partyId);

	@Modifying
    @Transactional
    @Query("UPDATE VoterEntity v SET v.pincode = NULL WHERE v.partNo = :partNo AND v.electionId = :electionId")
    int updatePincodeToNullByPartNoAndElectionId(String partNo, Long electionId);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VoterEntity v WHERE v.partManager.id = :partManagerId")
    boolean existsByPartManagerId(@Param("partManagerId") Long partManagerId);
	   
	@Query("SELECT v FROM VoterEntity v " +
		       "WHERE v.accountId = :accountId " +
		       "AND v.electionId = :electionId " +
		       "AND v.familyId IS NOT NULL " +
		       "AND (:partNumbers IS NULL OR v.familyId IN (" +
		       "    SELECT v2.familyId " +
		       "    FROM VoterEntity v2 " +
		       "    WHERE v2.accountId = :accountId " +
		       "    AND v2.electionId = :electionId " +
		       "    AND v2.partNo IN :partNumbers " +
		       "    AND (v2.age IS NOT NULL OR v2.dob IS NOT NULL) " +
		       "    AND NOT EXISTS (" +
		       "        SELECT 1 FROM VoterEntity v3 " +
		       "        WHERE v3.familyId = v2.familyId " +
		       "        AND v3.accountId = :accountId " +
		       "        AND v3.electionId = :electionId " +
		       "        AND (v3.age IS NOT NULL OR v3.dob IS NOT NULL) " +
		       "        AND (" +
		       "            (v3.age IS NOT NULL AND v2.age IS NOT NULL AND v3.age > v2.age) OR " +
		       "            (v3.age IS NOT NULL AND v2.age IS NULL) OR " +
		       "            (v3.age IS NULL AND v2.age IS NULL AND v3.dob IS NOT NULL AND v2.dob IS NOT NULL AND v3.dob < v2.dob)" +
		       "        )" +
		       "    )" +
		       "))")
		Page<VoterEntity> findByAccountIdAndElectionIdAndNonNullFamilyId(
		        @Param("accountId") Long accountId,
		        @Param("electionId") Long electionId,
		        @Param("partNumbers") List<Integer> partNumbers,
		        Pageable pageable);

	@Query("SELECT " +
		       "COALESCE(SUM(CASE WHEN LOWER(v.gender) = 'male' THEN 1 ELSE 0 END), 0) as maleCount, " +
		       "COALESCE(SUM(CASE WHEN LOWER(v.gender) = 'female' THEN 1 ELSE 0 END), 0) as femaleCount, " +
		       "COALESCE(SUM(CASE WHEN LOWER(v.gender) NOT IN ('male', 'female') THEN 1 ELSE 0 END), 0) as otherCount, " +
		       "COALESCE(COUNT(v), 0) as totalCount " +
		       "FROM VoterEntity v " +
		       "WHERE v.accountId = :accountId " +
		       "AND v.electionId = :electionId " +
		       "AND v.familyId IS NOT NULL " +
		       "AND (:partNumbers IS NULL OR v.familyId IN (" +
		       "    SELECT v2.familyId " +
		       "    FROM VoterEntity v2 " +
		       "    WHERE v2.accountId = :accountId " +
		       "    AND v2.electionId = :electionId " +
		       "    AND v2.partNo IN :partNumbers " +
		       "    AND (v2.age IS NOT NULL OR v2.dob IS NOT NULL) " +
		       "    AND NOT EXISTS (" +
		       "        SELECT 1 FROM VoterEntity v3 " +
		       "        WHERE v3.familyId = v2.familyId " +
		       "        AND v3.accountId = :accountId " +
		       "        AND v3.electionId = :electionId " +
		       "        AND (v3.age IS NOT NULL OR v3.dob IS NOT NULL) " +
		       "        AND (" +
		       "            (v3.age IS NOT NULL AND v2.age IS NOT NULL AND v3.age > v2.age) OR " +
		       "            (v3.age IS NOT NULL AND v2.age IS NULL) OR " +
		       "            (v3.age IS NULL AND v2.age IS NULL AND v3.dob IS NOT NULL AND v2.dob IS NOT NULL AND v3.dob < v2.dob)" +
		       "        )" +
		       "    )" +
		       "))")
		GenderStatsProjection getGenderStatsByFamily(
		        @Param("accountId") Long accountId,
		        @Param("electionId") Long electionId,
		        @Param("partNumbers") List<Integer> partNumbers);
	
	
	@Query("SELECT " +
		       "COALESCE(SUM(CASE WHEN LOWER(v.gender) = 'male' THEN 1 ELSE 0 END), 0) as maleCount, " +
		       "COALESCE(SUM(CASE WHEN LOWER(v.gender) = 'female' THEN 1 ELSE 0 END), 0) as femaleCount, " +
		       "COALESCE(SUM(CASE WHEN LOWER(v.gender) NOT IN ('male', 'female') THEN 1 ELSE 0 END), 0) as otherCount, " +
		       "COALESCE(COUNT(v), 0) as totalCount " +
		       "FROM VoterEntity v " +
		       "WHERE v.accountId = :accountId " +
		       "AND v.electionId = :electionId " +
		       "AND v.familyId IS NOT NULL " +
		       "AND (:partNumbers IS NULL OR v.familyId IN (" +
		       "    SELECT v2.familyId " +
		       "    FROM VoterEntity v2 " +
		       "    WHERE v2.accountId = :accountId " +
		       "    AND v2.electionId = :electionId " +
		       "    AND v2.partNo IN :partNumbers " +
		       "    AND (v2.age IS NOT NULL OR v2.dob IS NOT NULL) " +
		       "    AND NOT EXISTS (" +
		       "        SELECT 1 FROM VoterEntity v3 " +
		       "        WHERE v3.familyId = v2.familyId " +
		       "        AND v3.accountId = :accountId " +
		       "        AND v3.electionId = :electionId " +
		       "        AND (v3.age IS NOT NULL OR v3.dob IS NOT NULL) " +
		       "        AND (" +
		       "            (v3.age IS NOT NULL AND v2.age IS NOT NULL AND v3.age > v2.age) OR " +
		       "            (v3.age IS NOT NULL AND v2.age IS NULL) OR " +
		       "            (v3.age IS NULL AND v2.age IS NULL AND v3.dob IS NOT NULL AND v2.dob IS NOT NULL AND v3.dob < v2.dob)" +
		       "        )" +
		       "    )" +
		       ")) " +
		       "AND (:nameFilter IS NULL OR LOWER(v.voterFnameEn) LIKE LOWER(:nameFilter))")
		GenderStatsProjection getGenderStatsByFamilyAndName(
		        @Param("accountId") Long accountId,
		        @Param("electionId") Long electionId,
		        @Param("partNumbers") List<Integer> partNumbers,
		        @Param("nameFilter") String nameFilter);
	
	    


	    List<VoterEntity> findByElectionIdAndBoothNumberOrderBySerialNoAsc(Long electionId, Integer boothNumber);
		
//	    @Query("SELECT new com.thedal.thedal_app.voter.dto.VoterStatusDTO(v.serialNo, v.epicNumber, v.hasVoted, v.votedTimestamp) " +
//           "FROM VoterEntity v WHERE v.electionId = :electionId AND v.boothNumber = :boothNumber")
//        List<VoterStatusDTO> findSerialNosAndHasVotedByElectionIdAndBoothNumber(Long electionId, Integer boothNumber);
//		
//	    @Query("SELECT new com.thedal.thedal_app.voter.dto.VoterStatusDTO(v.serialNo, v.epicNumber, v.hasVoted, v.votedTimestamp) " +
//	            "FROM VoterEntity v " +
//	            "WHERE v.electionId = :electionId AND v.boothNumber = :boothNumber " +
//	            "ORDER BY v.serialNo ASC")
//	     List<VoterStatusDTO> findSerialNosAndHasVotedByElectionIdAndBoothNumber(
//	             @Param("electionId") Long electionId,
//	             @Param("boothNumber") Integer boothNumber);
		
	    @Query("SELECT new com.thedal.thedal_app.voter.dto.VoterStatusDTO(v.serialNo, v.epicNumber, v.hasVoted, v.votedTimestamp) " +
	    	       "FROM VoterEntity v " +
	    	       "WHERE v.electionId = :electionId AND v.boothNumber = :boothNumber " +
	    	       "AND (:pollStatus IS NULL OR " +
	    	       "     (:pollStatus = 'voted' AND v.hasVoted = true) OR " +
	    	       "     (:pollStatus = 'notVoted' AND (v.hasVoted = false OR v.hasVoted IS NULL)) OR " +
	    	       "     (:pollStatus = 'all'))")
	    	Page<VoterStatusDTO> findSerialNosAndHasVotedByElectionIdAndBoothNumber(
	    	        @Param("electionId") Long electionId,
	    	        @Param("boothNumber") Integer boothNumber,
	    	        @Param("pollStatus") String pollStatus,
	    	        Pageable pageable);
	    
	    @Query("SELECT " +
	            "SUM(CASE WHEN v.hasVoted = true THEN 1 ELSE 0 END) as votedCount, " +
	            "SUM(CASE WHEN v.hasVoted = false OR v.hasVoted IS NULL THEN 1 ELSE 0 END) as notVotedCount " +
	            "FROM VoterEntity v " +
	            "WHERE v.electionId = :electionId AND v.boothNumber = :boothNumber")
	     Map<String, Long> countVotersByVotingStatus(Long electionId, Integer boothNumber);	 
	    
	    @Query("SELECT COUNT(v) > 0 FROM VoterEntity v WHERE v.availability1.id = :availabilityId AND v.electionId = :electionId")
	    boolean existsByAvailability1IdAndElectionId(@Param("availabilityId") Long availabilityId, @Param("electionId") Long electionId);
	    
	 
	    @Query("SELECT v FROM VoterEntity v " +
	    	       "LEFT JOIN FETCH v.availability1 a " +
	    	       "LEFT JOIN FETCH v.party p " +
	    	       "LEFT JOIN FETCH v.religion r " +
	    	       "LEFT JOIN FETCH v.caste c " +
	    	       "LEFT JOIN FETCH v.subCaste sc " +
	    	       "LEFT JOIN FETCH v.casteCategory cc " +
	    	       "LEFT JOIN FETCH v.dynamicFieldEntity df " +
	    	       "LEFT JOIN v.partManager pm " +
	    	       "WHERE v.accountId = :accountId " +
	    	       "AND v.electionId = :electionId " +
	    	       "AND (:voterId IS NULL OR v.voterId = :voterId) " +
	    	       "AND (:epicNumber IS NULL OR v.epicNumber = :epicNumber) " +
	    	       "AND (:boothNumbers IS NULL OR v.boothNumber IN :boothNumbers) " +
	    	       "AND (:familyId IS NULL OR v.familyId = :familyId) " +
	    	       "AND (:friendId IS NULL OR v.friendId = :friendId) " +
	    	       "AND (:voterFnameEn IS NULL AND :voterLnameEn IS NULL AND :voterFnameL1 IS NULL AND :voterFnameL2 IS NULL AND :voterLnameL1 IS NULL AND :voterLnameL2 IS NULL OR " +
	    	       "     (:voterFnameEn IS NOT NULL AND LOWER(TRIM(v.voterFnameEn)) IN :voterFnameEn) OR " +
	    	       "     (:voterLnameEn IS NOT NULL AND LOWER(TRIM(v.voterLnameEn)) IN :voterLnameEn) OR " +
	    	       "     (:voterFnameL1 IS NOT NULL AND LOWER(TRIM(v.voterFnameL1)) IN :voterFnameL1) OR " +
	    	       "     (:voterFnameL2 IS NOT NULL AND LOWER(TRIM(v.voterFnameL2)) IN :voterFnameL2) OR " +
	    	       "     (:voterLnameL1 IS NOT NULL AND LOWER(TRIM(v.voterLnameL1)) IN :voterLnameL1) OR " +
	    	       "     (:voterLnameL2 IS NOT NULL AND LOWER(TRIM(v.voterLnameL2)) IN :voterLnameL2)) " +
	    	       "AND (:relationFirstNameEn IS NULL AND :relationLastNameEn IS NULL AND :rlnFnameL1 IS NULL AND :rlnFnameL2 IS NULL AND :rlnLnameL1 IS NULL AND :rlnLnameL2 IS NULL OR " +
	    	       "     (:relationFirstNameEn IS NOT NULL AND LOWER(TRIM(v.rlnFnameEn)) IN :relationFirstNameEn) OR " +
	    	       "     (:relationLastNameEn IS NOT NULL AND LOWER(TRIM(v.rlnLnameEn)) IN :relationLastNameEn) OR " +
	    	       "     (:rlnFnameL1 IS NOT NULL AND LOWER(TRIM(v.rlnFnameL1)) IN :rlnFnameL1) OR " +
	    	       "     (:rlnFnameL2 IS NOT NULL AND LOWER(TRIM(v.rlnFnameL2)) IN :rlnFnameL2) OR " +
	    	       "     (:rlnLnameL1 IS NOT NULL AND LOWER(TRIM(v.rlnLnameL1)) IN :rlnLnameL1) OR " +
	    	       "     (:rlnLnameL2 IS NOT NULL AND LOWER(TRIM(v.rlnLnameL2)) IN :rlnLnameL2)) " +
	    	       "AND (:voterHistoryName IS NULL OR EXISTS (SELECT 1 FROM VoterHistoryEntity vh JOIN vh.voters vhv WHERE vhv.id = v.id AND LOWER(TRIM(vh.voterHistoryName)) IN :voterHistoryName)) " +
	    	       "AND (:partyName IS NULL OR LOWER(TRIM(p.partyName)) IN :partyName) " +
	    	       "AND (:religionName IS NULL OR LOWER(TRIM(r.religionName)) IN :religionName) " +
	    	       "AND (:age IS NULL OR v.age = :age) " +
				   "AND (" +
				   "    ((:includeUnknownAge IS NULL OR :includeUnknownAge = TRUE) AND " +
				   "        (v.age IS NULL OR ((:minAge IS NULL OR v.age >= :minAge) AND (:maxAge IS NULL OR v.age <= :maxAge)))) " +
				   "    OR " +
				   "    (:includeUnknownAge = FALSE AND v.age IS NOT NULL AND " +
				   "        (:minAge IS NULL OR v.age >= :minAge) AND (:maxAge IS NULL OR v.age <= :maxAge))" +
				   ") " +
	    	       "AND (:genders IS NULL OR LOWER(v.gender) IN :genders) " +
//	    	       "AND (:hasDob IS NULL OR (" +
//	    	       "    :hasDob = true AND v.dob IS NOT NULL AND (" +
//	    	       "        (month(v.dob) = month(CURRENT_DATE) AND day(v.dob) = day(CURRENT_DATE)) OR " +
//	    	       "        (month(v.dob) = :tomorrowMonth AND day(v.dob) = :tomorrowDay)" +
//	    	       "    ))) " +
				   "AND ((COALESCE(:filterToday, false) = false AND COALESCE(:filterTomorrow, false) = false AND :birthdayMonth IS NULL AND :birthdayDay IS NULL) OR " +
				   "    (v.dob IS NOT NULL AND (" +
				   "        (COALESCE(:filterToday, false) = true AND month(v.dob) = :todayMonth AND day(v.dob) = :todayDay) OR " +
				   "        (COALESCE(:filterTomorrow, false) = true AND month(v.dob) = :tomorrowMonth AND day(v.dob) = :tomorrowDay) OR " +
				   "        (:birthdayMonth IS NOT NULL AND :birthdayDay IS NOT NULL AND month(v.dob) = :birthdayMonth AND day(v.dob) = :birthdayDay)" +
				   "    ))) " +
	    	       "AND (:starNumber IS NULL OR v.starNumber = :starNumber) " +
	    	       "AND (:description IS NULL OR LOWER(TRIM(a.description)) IN :description) " +
	    	       "AND (:categoryName IS NULL OR LOWER(TRIM(a.categoryName)) IN :categoryName) " +
	    	       "AND (:casteCategoryName IS NULL OR LOWER(TRIM(cc.casteCategoryName)) IN :casteCategoryName) " +
	    	       "AND (:casteName IS NULL OR LOWER(TRIM(c.casteName)) IN :casteName) " +
	    	       "AND (:subCasteName IS NULL OR LOWER(TRIM(sc.subCasteName)) IN :subCasteName) " +
	    	       "AND (:overseas IS NULL OR " +
	    	       "     (:overseas = true AND v.sectionNo = 999) OR " +
	    	       "     (:overseas = false AND (v.sectionNo IS NULL OR v.sectionNo != 999))) " +
	    	       "AND (:serialNo IS NULL OR v.serialNo = :serialNo) " +
	    	       "AND (:fatherless IS NULL OR (:fatherless = true AND v.rlnType IS NOT NULL AND LOWER(v.rlnType) = 'mother')) " +
	    	       "AND (:guardian IS NULL OR (:guardian = true AND v.rlnType IS NOT NULL AND LOWER(v.rlnType) = 'other'))"+
	    		   "AND (:hasMobileNo IS NULL OR (:hasMobileNo = true AND v.mobileNo IS NOT NULL AND TRIM(v.mobileNo) != ''))"+
	    		   "AND (:mobileNo IS NULL OR v.mobileNo = :mobileNo OR v.whatsappNo = :mobileNo) " +
	    		   "AND (:singleVoterFamily IS NULL OR (:singleVoterFamily = true AND v.familyId IS NOT NULL AND " +
	    	       "     (SELECT COUNT(v2) FROM VoterEntity v2 WHERE v2.familyId = v.familyId AND v2.accountId = :accountId AND v2.electionId = :electionId) = 1)) " +
	    	       "AND (:pollStatus IS NULL OR " +
	    	       "     (:pollStatus = 'voted' AND v.hasVoted = true) OR " +
	    	       "     (:pollStatus = 'notVoted' AND (v.hasVoted = false OR v.hasVoted IS NULL)) OR " +
	    	       "     (:pollStatus = 'all')) " +
	    	       "AND (:isFamily IS NULL OR (:isFamily = true AND v.familyId IS NOT NULL) OR (:isFamily = false AND v.familyId IS NULL)) " +
	    	       "ORDER BY " +
	    	       "CASE WHEN (:filterToday = true AND :filterTomorrow = true) THEN " +
	    	       "     CASE WHEN (month(v.dob) = :todayMonth AND day(v.dob) = :todayDay) THEN 0 " +
	    	       "          WHEN (month(v.dob) = :tomorrowMonth AND day(v.dob) = :tomorrowDay) THEN 1 " +
	    	       "          ELSE 2 END " +
	    	       "     ELSE 0 END, " +
	    	       "v.partNo, v.serialNo")
	    	Page<VoterEntity> findByAccountIdAndElectionIdAndFiltersOptimized(
	    	    @Param("accountId") Long accountId,
	    	    @Param("electionId") Long electionId,
	    	    @Param("voterId") String voterId,
	    	    @Param("epicNumber") String epicNumber,
	    	    @Param("boothNumbers") List<Integer> boothNumbers,
	    	    @Param("familyId") UUID familyId,
	    	    @Param("friendId") UUID friendId,
	    	    @Param("voterFnameEn") List<String> voterFnameEn,
	    	    @Param("voterLnameEn") List<String> voterLnameEn,
	    	    @Param("voterFnameL1") List<String> voterFnameL1,
	    	    @Param("voterFnameL2") List<String> voterFnameL2,
	    	    @Param("voterLnameL1") List<String> voterLnameL1,
	    	    @Param("voterLnameL2") List<String> voterLnameL2,
	    	    @Param("relationFirstNameEn") List<String> relationFirstNameEn,
	    	    @Param("relationLastNameEn") List<String> relationLastNameEn,
	    	    @Param("rlnFnameL1") List<String> rlnFnameL1,
	    	    @Param("rlnFnameL2") List<String> rlnFnameL2,
	    	    @Param("rlnLnameL1") List<String> rlnLnameL1,
	    	    @Param("rlnLnameL2") List<String> rlnLnameL2,
	    	    @Param("partyName") List<String> partyName,
	    	    @Param("voterHistoryName") List<String> voterHistoryName,
	    	    @Param("religionName") List<String> religionName,
	    	    @Param("age") Integer age,
	    	    @Param("minAge") Integer minAge,
	    	    @Param("maxAge") Integer maxAge,
	    	    @Param("includeUnknownAge") Boolean includeUnknownAge,
	    	    @Param("genders") List<String> genders,
	    	    @Param("filterToday") Boolean filterToday,
	    	    @Param("filterTomorrow") Boolean filterTomorrow,
	    	    @Param("todayMonth") Integer todayMonth,
	    	    @Param("todayDay") Integer todayDay,
	    	    @Param("tomorrowMonth") Integer tomorrowMonth,
			    @Param("tomorrowDay") Integer tomorrowDay,
			    @Param("birthdayMonth") Integer birthdayMonth,
			    @Param("birthdayDay") Integer birthdayDay,
	    	    @Param("starNumber") Boolean starNumber,
	    	    @Param("description") List<String> description,
	    	    @Param("categoryName") List<String> categoryName,
	    	    @Param("casteCategoryName") List<String> casteCategoryName,
	    	    @Param("casteName") List<String> casteName,
	    	    @Param("subCasteName") List<String> subCasteName,
	    	    @Param("serialNo") Long serialNo,
	    	    @Param("overseas") Boolean overseas,
	    	    @Param("fatherless") Boolean fatherless,
	    	    @Param("guardian") Boolean guardian,
	    	    @Param("hasMobileNo") Boolean hasMobileNo,
	    	    @Param("mobileNo") String mobileNo,
	    	    @Param("singleVoterFamily") Boolean singleVoterFamily,
	    	    @Param("pollStatus") String pollStatus,
	    	    @Param("isFamily") Boolean isFamily,
	    	    Pageable pageable);

	    // New method for dynamic sorting without hard-coded ORDER BY for main voters API
	    @Query("SELECT v FROM VoterEntity v " +
		    "LEFT JOIN v.availability1 a " +
		    "LEFT JOIN v.party p " +
		    "LEFT JOIN v.religion r " +
		    "LEFT JOIN v.partManager pm " +
		    "LEFT JOIN v.casteCategory cc " +
		    "LEFT JOIN v.caste c " +
		    "LEFT JOIN v.subCaste sc " +
		    "LEFT JOIN v.dynamicFieldEntity df " +
		    "WHERE v.accountId = :accountId " +
		    "AND v.electionId = :electionId " +
		    "AND (:voterId IS NULL OR v.voterId = :voterId) " +
		    "AND (:epicNumber IS NULL OR v.epicNumber = :epicNumber) " +
		    "AND (:boothNumbers IS NULL OR v.partNo IN :boothNumbers) " +
		    "AND (:familyId IS NULL OR v.familyId = :familyId) " +
		    "AND (:friendId IS NULL OR v.friendId = :friendId) " +
		    "AND (:voterFnameEn IS NULL OR LOWER(TRIM(v.voterFnameEn)) IN :voterFnameEn) " +
		    "AND (:voterLnameEn IS NULL OR LOWER(TRIM(v.voterLnameEn)) IN :voterLnameEn) " +
		    "AND (:voterFnameL1 IS NULL OR LOWER(TRIM(v.voterFnameL1)) IN :voterFnameL1) " +
		    "AND (:voterFnameL2 IS NULL OR LOWER(TRIM(v.voterFnameL2)) IN :voterFnameL2) " +
		    "AND (:voterLnameL1 IS NULL OR LOWER(TRIM(v.voterLnameL1)) IN :voterLnameL1) " +
		    "AND (:voterLnameL2 IS NULL OR LOWER(TRIM(v.voterLnameL2)) IN :voterLnameL2) " +
		    "AND (:relationFirstNameEn IS NULL OR LOWER(TRIM(v.rlnFnameEn)) IN :relationFirstNameEn) " +
		    "AND (:relationLastNameEn IS NULL OR LOWER(TRIM(v.rlnLnameEn)) IN :relationLastNameEn) " +
		    "AND (:rlnFnameL1 IS NULL OR LOWER(TRIM(v.rlnFnameL1)) IN :rlnFnameL1) " +
		    "AND (:rlnFnameL2 IS NULL OR LOWER(TRIM(v.rlnFnameL2)) IN :rlnFnameL2) " +
		    "AND (:rlnLnameL1 IS NULL OR LOWER(TRIM(v.rlnLnameL1)) IN :rlnLnameL1) " +
		    "AND (:rlnLnameL2 IS NULL OR LOWER(TRIM(v.rlnLnameL2)) IN :rlnLnameL2) " +
		    "AND (:voterHistoryName IS NULL OR EXISTS (SELECT 1 FROM VoterHistoryEntity vh JOIN vh.voters vhv WHERE vhv.id = v.id AND LOWER(TRIM(vh.voterHistoryName)) IN :voterHistoryName)) " +
		    "AND (:partyName IS NULL OR LOWER(TRIM(p.partyName)) IN :partyName) " +
		    "AND (:religionName IS NULL OR LOWER(TRIM(r.religionName)) IN :religionName) " +
		    "AND (:age IS NULL OR v.age = :age) " +
		    "AND (" +
		    "    ((:includeUnknownAge IS NULL OR :includeUnknownAge = TRUE) AND " +
		    "        (v.age IS NULL OR ((:minAge IS NULL OR v.age >= :minAge) AND (:maxAge IS NULL OR v.age <= :maxAge)))) " +
		    "    OR " +
		    "    (:includeUnknownAge = FALSE AND v.age IS NOT NULL AND " +
		    "        (:minAge IS NULL OR v.age >= :minAge) AND (:maxAge IS NULL OR v.age <= :maxAge))" +
		    ") " +
		    "AND (:genders IS NULL OR LOWER(v.gender) IN :genders) " +
		    "AND ((COALESCE(:filterToday, false) = false AND COALESCE(:filterTomorrow, false) = false AND :birthdayMonth IS NULL AND :birthdayDay IS NULL) OR " +
		    "    (v.dob IS NOT NULL AND (" +
		    "        (COALESCE(:filterToday, false) = true AND month(v.dob) = :todayMonth AND day(v.dob) = :todayDay) OR " +
		    "        (COALESCE(:filterTomorrow, false) = true AND month(v.dob) = :tomorrowMonth AND day(v.dob) = :tomorrowDay) OR " +
		    "        (:birthdayMonth IS NOT NULL AND :birthdayDay IS NOT NULL AND month(v.dob) = :birthdayMonth AND day(v.dob) = :birthdayDay)" +
		    "    ))) " +
		    "AND (:starNumber IS NULL OR v.starNumber = :starNumber) " +
		    "AND (:description IS NULL OR LOWER(TRIM(a.description)) IN :description) " +
		    "AND (:categoryName IS NULL OR LOWER(TRIM(a.categoryName)) IN :categoryName) " +
		    "AND (:casteCategoryName IS NULL OR LOWER(TRIM(cc.casteCategoryName)) IN :casteCategoryName) " +
		    "AND (:casteName IS NULL OR LOWER(TRIM(c.casteName)) IN :casteName) " +
		    "AND (:subCasteName IS NULL OR LOWER(TRIM(sc.subCasteName)) IN :subCasteName) " +
		    "AND (:overseas IS NULL OR " +
		    "     (:overseas = true AND v.sectionNo = 999) OR " +
		    "     (:overseas = false AND (v.sectionNo IS NULL OR v.sectionNo != 999))) " +
		    "AND (:serialNo IS NULL OR v.serialNo = :serialNo) " +
		    "AND (:fatherless IS NULL OR (:fatherless = true AND v.rlnType IS NOT NULL AND LOWER(v.rlnType) = 'mother')) " +
		    "AND (:guardian IS NULL OR (:guardian = true AND v.rlnType IS NOT NULL AND LOWER(v.rlnType) = 'other'))"+
		    "AND (:hasMobileNo IS NULL OR (:hasMobileNo = true AND v.mobileNo IS NOT NULL AND TRIM(v.mobileNo) != ''))"+
		    "AND (:mobileNo IS NULL OR v.mobileNo = :mobileNo OR v.whatsappNo = :mobileNo) " +
		    "AND (:singleVoterFamily IS NULL OR (:singleVoterFamily = true AND v.familyId IS NOT NULL AND " +
		    "     (SELECT COUNT(v2) FROM VoterEntity v2 WHERE v2.familyId = v.familyId AND v2.accountId = :accountId AND v2.electionId = :electionId) = 1)) " +
		    "AND (:pollStatus IS NULL OR " +
		    "     (:pollStatus = 'voted' AND v.hasVoted = true) OR " +
		    "     (:pollStatus = 'notVoted' AND (v.hasVoted = false OR v.hasVoted IS NULL)) OR " +
		    "     (:pollStatus = 'all')) " +
		    "AND (:isFamily IS NULL OR (:isFamily = true AND v.familyId IS NOT NULL) OR (:isFamily = false AND v.familyId IS NULL))")
	    	Page<VoterEntity> findByAccountIdAndElectionIdAndFiltersWithDynamicSort(
	    	    @Param("accountId") Long accountId,
	    	    @Param("electionId") Long electionId,
	    	    @Param("voterId") String voterId,
	    	    @Param("epicNumber") String epicNumber,
	    	    @Param("boothNumbers") List<Integer> boothNumbers,
	    	    @Param("familyId") UUID familyId,
	    	    @Param("friendId") UUID friendId,
	    	    @Param("voterFnameEn") List<String> voterFnameEn,
	    	    @Param("voterLnameEn") List<String> voterLnameEn,
	    	    @Param("voterFnameL1") List<String> voterFnameL1,
	    	    @Param("voterFnameL2") List<String> voterFnameL2,
	    	    @Param("voterLnameL1") List<String> voterLnameL1,
	    	    @Param("voterLnameL2") List<String> voterLnameL2,
	    	    @Param("relationFirstNameEn") List<String> relationFirstNameEn,
	    	    @Param("relationLastNameEn") List<String> relationLastNameEn,
	    	    @Param("rlnFnameL1") List<String> rlnFnameL1,
	    	    @Param("rlnFnameL2") List<String> rlnFnameL2,
	    	    @Param("rlnLnameL1") List<String> rlnLnameL1,
	    	    @Param("rlnLnameL2") List<String> rlnLnameL2,
	    	    @Param("partyName") List<String> partyName,
	    	    @Param("voterHistoryName") List<String> voterHistoryName,
	    	    @Param("religionName") List<String> religionName,
	    	    @Param("age") Integer age,
	    	    @Param("minAge") Integer minAge,
	    	    @Param("maxAge") Integer maxAge,
	    	    @Param("includeUnknownAge") Boolean includeUnknownAge,
	    	    @Param("genders") List<String> genders,
	    	    @Param("filterToday") Boolean filterToday,
	    	    @Param("filterTomorrow") Boolean filterTomorrow,
	    	    @Param("todayMonth") Integer todayMonth,
	    	    @Param("todayDay") Integer todayDay,
	    	    @Param("tomorrowMonth") Integer tomorrowMonth,
		    @Param("tomorrowDay") Integer tomorrowDay,
		    @Param("birthdayMonth") Integer birthdayMonth,
		    @Param("birthdayDay") Integer birthdayDay,
	    	    @Param("starNumber") Boolean starNumber,
	    	    @Param("description") List<String> description,
	    	    @Param("categoryName") List<String> categoryName,
	    	    @Param("casteCategoryName") List<String> casteCategoryName,
	    	    @Param("casteName") List<String> casteName,
	    	    @Param("subCasteName") List<String> subCasteName,
	    	    @Param("serialNo") Long serialNo,
	    	    @Param("overseas") Boolean overseas,
	    	    @Param("fatherless") Boolean fatherless,
	    	    @Param("guardian") Boolean guardian,
	    	    @Param("hasMobileNo") Boolean hasMobileNo,
	    	    @Param("mobileNo") String mobileNo,
	    	    @Param("singleVoterFamily") Boolean singleVoterFamily,
	    	    @Param("pollStatus") String pollStatus,
	    	    @Param("isFamily") Boolean isFamily,
	    	    Pageable pageable);

	    @Query("SELECT " +
	    	       "COALESCE(COUNT(DISTINCT CASE WHEN LOWER(TRIM(v.gender)) IN ('male', 'm') THEN v.id END), 0) as maleCount, " +
	    	       "COALESCE(COUNT(DISTINCT CASE WHEN LOWER(TRIM(v.gender)) IN ('female', 'f') THEN v.id END), 0) as femaleCount, " +
	    	       "COALESCE(COUNT(DISTINCT CASE WHEN LOWER(TRIM(v.gender)) NOT IN ('male', 'm', 'female', 'f') AND v.gender IS NOT NULL THEN v.id END), 0) as otherCount, " +
	    	       "COALESCE(COUNT(DISTINCT v), 0) as totalCount " +
	    	       "FROM VoterEntity v " +
	    	       "LEFT JOIN v.availability1 a " +
	    	       "LEFT JOIN v.party p " +
	    	       "LEFT JOIN v.religion r " +
	    	       "LEFT JOIN v.partManager pm " +
	    	       "LEFT JOIN v.casteCategory cc " +
	    	       "LEFT JOIN v.caste c " +
	    	       "LEFT JOIN v.subCaste sc " +
	    	       "LEFT JOIN v.dynamicFieldEntity df " +
	    	       "WHERE v.accountId = :accountId " +
	    	       "AND v.electionId = :electionId " +
	    	       "AND (:voterId IS NULL OR v.voterId = :voterId) " +
	    	       "AND (:epicNumber IS NULL OR v.epicNumber = :epicNumber) " +
	    	       "AND (:boothNumbers IS NULL OR v.boothNumber IN :boothNumbers) " +
	    	       "AND (:familyId IS NULL OR v.familyId = :familyId) " +
	    	       "AND (:friendId IS NULL OR v.friendId = :friendId) " +
	    	       "AND (:voterFnameEn IS NULL AND :voterLnameEn IS NULL AND :voterFnameL1 IS NULL AND :voterFnameL2 IS NULL AND :voterLnameL1 IS NULL AND :voterLnameL2 IS NULL OR " +
	    	       "     (:voterFnameEn IS NOT NULL AND LOWER(TRIM(v.voterFnameEn)) IN :voterFnameEn) OR " +
	    	       "     (:voterLnameEn IS NOT NULL AND LOWER(TRIM(v.voterLnameEn)) IN :voterLnameEn) OR " +
	    	       "     (:voterFnameL1 IS NOT NULL AND LOWER(TRIM(v.voterFnameL1)) IN :voterFnameL1) OR " +
	    	       "     (:voterFnameL2 IS NOT NULL AND LOWER(TRIM(v.voterFnameL2)) IN :voterFnameL2) OR " +
	    	       "     (:voterLnameL1 IS NOT NULL AND LOWER(TRIM(v.voterLnameL1)) IN :voterLnameL1) OR " +
	    	       "     (:voterLnameL2 IS NOT NULL AND LOWER(TRIM(v.voterLnameL2)) IN :voterLnameL2)) " +
	    	       "AND (:relationFirstNameEn IS NULL AND :relationLastNameEn IS NULL AND :rlnFnameL1 IS NULL AND :rlnFnameL2 IS NULL AND :rlnLnameL1 IS NULL AND :rlnLnameL2 IS NULL OR " +
	    	       "     (:relationFirstNameEn IS NOT NULL AND LOWER(TRIM(v.rlnFnameEn)) IN :relationFirstNameEn) OR " +
	    	       "     (:relationLastNameEn IS NOT NULL AND LOWER(TRIM(v.rlnLnameEn)) IN :relationLastNameEn) OR " +
	    	       "     (:rlnFnameL1 IS NOT NULL AND LOWER(TRIM(v.rlnFnameL1)) IN :rlnFnameL1) OR " +
	    	       "     (:rlnFnameL2 IS NOT NULL AND LOWER(TRIM(v.rlnFnameL2)) IN :rlnFnameL2) OR " +
	    	       "     (:rlnLnameL1 IS NOT NULL AND LOWER(TRIM(v.rlnLnameL1)) IN :rlnLnameL1) OR " +
	    	       "     (:rlnLnameL2 IS NOT NULL AND LOWER(TRIM(v.rlnLnameL2)) IN :rlnLnameL2)) " +
	    	       "AND (:partyName IS NULL OR (p IS NOT NULL AND LOWER(TRIM(p.partyName)) IN :partyName)) " +
	    	       "AND (:voterHistoryName IS NULL OR EXISTS (SELECT 1 FROM VoterHistoryEntity vh JOIN vh.voters vhv WHERE vhv.id = v.id AND LOWER(TRIM(vh.voterHistoryName)) IN :voterHistoryName)) " +
	    	       "AND (:religionName IS NULL OR LOWER(TRIM(r.religionName)) IN :religionName) " +
	    	       "AND (:age IS NULL OR v.age = :age) " +
				   "AND (" +
				   "    ((:includeUnknownAge IS NULL OR :includeUnknownAge = TRUE) AND " +
				   "        (v.age IS NULL OR ((:minAge IS NULL OR v.age >= :minAge) AND (:maxAge IS NULL OR v.age <= :maxAge)))) " +
				   "    OR " +
				   "    (:includeUnknownAge = FALSE AND v.age IS NOT NULL AND " +
				   "        (:minAge IS NULL OR v.age >= :minAge) AND (:maxAge IS NULL OR v.age <= :maxAge))" +
				   ") " +
	    	       "AND (:genders IS NULL OR LOWER(TRIM(v.gender)) IN :genders) " +
//	    	       "AND (:hasDob IS NULL OR (" +
//	    	       "    :hasDob = true AND v.dob IS NOT NULL AND (" +
//	    	       "        (month(v.dob) = month(CURRENT_DATE) AND day(v.dob) = day(CURRENT_DATE)) OR " +
//	    	       "        (month(v.dob) = :tomorrowMonth AND day(v.dob) = :tomorrowDay)" +
//	    	       "    ))) " +
				   "AND ((COALESCE(:filterToday, false) = false AND COALESCE(:filterTomorrow, false) = false AND :birthdayMonth IS NULL AND :birthdayDay IS NULL) OR " +
				   "    (v.dob IS NOT NULL AND (" +
				   "        (COALESCE(:filterToday, false) = true AND month(v.dob) = :todayMonth AND day(v.dob) = :todayDay) OR " +
				   "        (COALESCE(:filterTomorrow, false) = true AND month(v.dob) = :tomorrowMonth AND day(v.dob) = :tomorrowDay) OR " +
				   "        (:birthdayMonth IS NOT NULL AND :birthdayDay IS NOT NULL AND month(v.dob) = :birthdayMonth AND day(v.dob) = :birthdayDay)" +
				   "    ))) " +
	    	       "AND (:starNumber IS NULL OR v.starNumber = :starNumber) " +
	    	       "AND (:description IS NULL OR LOWER(TRIM(a.description)) IN :description) " +
	    	       "AND (:overseas IS NULL OR " +
	    	       "     (:overseas = true AND v.sectionNo = 999) OR " +
	    	       "     (:overseas = false AND (v.sectionNo IS NULL OR v.sectionNo != 999))) " +
	    	       "AND (:fatherless IS NULL OR (:fatherless = true AND v.rlnType IS NOT NULL AND LOWER(v.rlnType) = 'mother')) " +
	    	       "AND (:guardian IS NULL OR (:guardian = true AND v.rlnType IS NOT NULL AND LOWER(v.rlnType) = 'other')) " +
	    	       "AND (:categoryName IS NULL OR LOWER(TRIM(a.categoryName)) IN :categoryName)"+
	    	       "AND (:casteCategoryName IS NULL OR LOWER(TRIM(cc.casteCategoryName)) IN :casteCategoryName)"+
	    	       "AND (:casteName IS NULL OR LOWER(TRIM(c.casteName)) IN :casteName)"+
	    	       "AND (:subCasteName IS NULL OR LOWER(TRIM(sc.subCasteName)) IN :subCasteName)"+
	    		   "AND (:hasMobileNo IS NULL OR (:hasMobileNo = true AND v.mobileNo IS NOT NULL AND TRIM(v.mobileNo) != ''))"+
	    		   "AND (:mobileNo IS NULL OR v.mobileNo = :mobileNo OR v.whatsappNo = :mobileNo) " +
	    		   "AND (:singleVoterFamily IS NULL OR (:singleVoterFamily = true AND v.familyId IS NOT NULL AND " +
	    	       "     (SELECT COUNT(v2) FROM VoterEntity v2 WHERE v2.familyId = v.familyId AND v2.accountId = :accountId AND v2.electionId = :electionId) = 1)) " +
	    	       "AND (:pollStatus IS NULL OR " +
	    	       "     (:pollStatus = 'voted' AND v.hasVoted = true) OR " +
	    	       "     (:pollStatus = 'notVoted' AND (v.hasVoted = false OR v.hasVoted IS NULL)) OR " +
	    	       "     (:pollStatus = 'all'))")
	    	GenderStatsProjection getFilteredGenderStats(
	    	    @Param("accountId") Long accountId,
	    	    @Param("electionId") Long electionId,
	    	    @Param("voterId") String voterId,
	    	    @Param("epicNumber") String epicNumber,
	    	    @Param("boothNumbers") List<Integer> boothNumbers,
	    	    @Param("familyId") UUID familyId,
	    	    @Param("friendId") UUID friendId,
	    	    @Param("voterFnameEn") List<String> voterFnameEn,
	    	    @Param("voterLnameEn") List<String> voterLnameEn,
	    	    @Param("voterFnameL1") List<String> voterFnameL1,
	    	    @Param("voterFnameL2") List<String> voterFnameL2,
	    	    @Param("voterLnameL1") List<String> voterLnameL1,
	    	    @Param("voterLnameL2") List<String> voterLnameL2,
	    	    @Param("relationFirstNameEn") List<String> relationFirstNameEn,
	    	    @Param("relationLastNameEn") List<String> relationLastNameEn,
	    	    @Param("rlnFnameL1") List<String> rlnFnameL1,
	    	    @Param("rlnFnameL2") List<String> rlnFnameL2,
	    	    @Param("rlnLnameL1") List<String> rlnLnameL1,
	    	    @Param("rlnLnameL2") List<String> rlnLnameL2,
	    	    @Param("partyName") List<String> partyName,
	    	    @Param("religionName") List<String> religionName,
	    	    @Param("voterHistoryName") List<String> voterHistoryName,
	    	    @Param("age") Integer age,
	    	    @Param("minAge") Integer minAge,
	    	    @Param("maxAge") Integer maxAge,
	    	    @Param("includeUnknownAge") Boolean includeUnknownAge,
	    	    @Param("genders") List<String> genders,
	    	    @Param("filterToday") Boolean filterToday,
	    	    @Param("filterTomorrow") Boolean filterTomorrow,
	    	    @Param("todayMonth") Integer todayMonth,
	    	    @Param("todayDay") Integer todayDay,
	    	    @Param("tomorrowMonth") Integer tomorrowMonth,
			    @Param("tomorrowDay") Integer tomorrowDay,
			    @Param("birthdayMonth") Integer birthdayMonth,
			    @Param("birthdayDay") Integer birthdayDay,
	    	    @Param("starNumber") Boolean starNumber,
	    	    @Param("description") List<String> description,
	    	    @Param("overseas") Boolean overseas,
	    	    @Param("fatherless") Boolean fatherless,
	    	    @Param("guardian") Boolean guardian,
	    	    @Param("categoryName") List<String> categoryName,
	    	    @Param("casteName") List<String> casteName,
	    	    @Param("subCasteName") List<String> subCasteName,
	    	    @Param("casteCategoryName") List<String> casteCategoryName,
	    	    @Param("hasMobileNo") Boolean hasMobileNo,
	    	    @Param("mobileNo") String mobileNo,
	    	    @Param("singleVoterFamily") Boolean singleVoterFamily,
	    	    @Param("pollStatus") String pollStatus);
	    
	    @Query("SELECT " +
	    	       "COALESCE(COUNT(DISTINCT CASE WHEN LOWER(TRIM(v.gender)) IN ('male', 'm') THEN v.id END), 0) as maleCount, " +
	    	       "COALESCE(COUNT(DISTINCT CASE WHEN LOWER(TRIM(v.gender)) IN ('female', 'f') THEN v.id END), 0) as femaleCount, " +
	    	       "COALESCE(COUNT(DISTINCT CASE WHEN LOWER(TRIM(v.gender)) NOT IN ('male', 'm', 'female', 'f') AND v.gender IS NOT NULL THEN v.id END), 0) as otherCount, " +
	    	       "COALESCE(COUNT(DISTINCT v), 0) as totalCount " +
	    	       "FROM VoterEntity v " +
	    	       "WHERE v.accountId = :accountId " +
	    	       "AND v.electionId = :electionId " +
	    	       "AND (:casteName IS NULL OR v.caste.casteName = :casteName) " +
	    	       "AND (:subCasteName IS NULL OR v.subCaste.subCasteName = :subCasteName)")
	    	GenderStatsProjection getCasteGenderStats(
	    	    @Param("accountId") Long accountId,
	    	    @Param("electionId") Long electionId,
	    	    @Param("casteName") String casteName,
	    	    @Param("subCasteName") String subCasteName);

	    
	    // DEBUG: Test method to find voters with any voter histories
	    @Query("SELECT " +
	    	       "COALESCE(COUNT(DISTINCT CASE WHEN LOWER(TRIM(v.gender)) IN ('male', 'm') THEN v.id END), 0) as maleCount, " +
	    	       "COALESCE(COUNT(DISTINCT CASE WHEN LOWER(TRIM(v.gender)) IN ('female', 'f') THEN v.id END), 0) as femaleCount, " +
	    	       "COALESCE(COUNT(DISTINCT CASE WHEN LOWER(TRIM(v.gender)) NOT IN ('male', 'm', 'female', 'f') AND v.gender IS NOT NULL THEN v.id END), 0) as otherCount, " +
	    	       "COALESCE(COUNT(DISTINCT v), 0) as totalCount " +
	    	       "FROM VoterEntity v " +
	    	       "LEFT JOIN v.availability1 a " +
	    	       "LEFT JOIN v.party p " +
	    	       "LEFT JOIN v.religion r " +
	    	       "LEFT JOIN v.partManager pm " +
	    	       "LEFT JOIN v.casteCategory cc " +
	    	       "WHERE v.accountId = :accountId " +
	    	       "AND v.electionId = :electionId " +
	    	       "AND (:voterId IS NULL OR v.voterId = :voterId) " +
	    	       "AND (:epicNumber IS NULL OR v.epicNumber = :epicNumber) " +
	    	       "AND (:boothNumbers IS NULL OR v.boothNumber IN :boothNumbers) " +
	    	       "AND (:familyId IS NULL OR v.familyId = :familyId) " +
	    	       "AND (:friendId IS NULL OR v.friendId = :friendId) " +
	    	       "AND (:voterFnameEn IS NULL OR LOWER(TRIM(v.voterFnameEn)) IN :voterFnameEn) " +
	    	       "AND (:voterLnameEn IS NULL OR LOWER(TRIM(v.voterLnameEn)) IN :voterLnameEn) " +
	    	       "AND (:voterFnameL1 IS NULL OR TRIM(v.voterFnameL1) IN :voterFnameL1) " +
	    	       "AND (:voterFnameL2 IS NULL OR TRIM(v.voterFnameL2) IN :voterFnameL2) " +
	    	       "AND (:relationFirstNameEn IS NULL OR LOWER(TRIM(v.rlnFnameEn)) IN :relationFirstNameEn) " +
	    	       "AND (:relationLastNameEn IS NULL OR LOWER(TRIM(v.rlnLnameEn)) IN :relationLastNameEn) " +
                   "AND (:rlnFnameL1 IS NULL OR TRIM(v.rlnFnameL1) IN :rlnFnameL1) " +
                   "AND (:rlnFnameL2 IS NULL OR TRIM(v.rlnFnameL2) IN :rlnFnameL2) " +
                   "AND (:rlnLnameL1 IS NULL OR TRIM(v.rlnLnameL1) IN :rlnLnameL1) " +
                   "AND (:rlnLnameL2 IS NULL OR TRIM(v.rlnLnameL2) IN :rlnLnameL2) " +
	    	       "AND (:partyName IS NULL OR (p IS NOT NULL AND LOWER(TRIM(p.partyName)) IN :partyName)) " +
	    	       "AND (:voterHistoryName IS NULL OR SIZE(v.voterHistories) > 0 AND EXISTS (SELECT 1 FROM VoterHistoryEntity vh JOIN vh.voters vhv WHERE vhv.id = v.id AND LOWER(TRIM(vh.voterHistoryName)) IN :voterHistoryName)) " +
	    	       "AND (:religionName IS NULL OR LOWER(TRIM(r.religionName)) IN :religionName) " +
	    	       "AND (:age IS NULL OR v.age = :age) " +
	    	       "AND (" +
	    	       "    (:includeUnknownAge = TRUE AND (v.age IS NULL OR " +
	    	       "     (v.age IS NOT NULL AND (:minAge IS NULL OR v.age >= :minAge) AND (:maxAge IS NULL OR v.age <= :maxAge)))) OR " +
	    	       "    (:includeUnknownAge = FALSE AND v.age IS NOT NULL AND " +
	    	       "     (:minAge IS NULL OR v.age >= :minAge) AND (:maxAge IS NULL OR v.age <= :maxAge))" +
	    	       ") " +
	    	       "AND (:genders IS NULL OR LOWER(TRIM(v.gender)) IN :genders) " +
	    	       "AND (:hasDob IS NULL OR (" +
	    	       "    :hasDob = true AND v.dob IS NOT NULL AND (" +
	    	       "        (month(v.dob) = month(CURRENT_DATE) AND day(v.dob) = day(CURRENT_DATE)) OR " +
	    	       "        (month(v.dob) = :tomorrowMonth AND day(v.dob) = :tomorrowDay)" +
	    	       "    )" +
	    	       ")) " +
	    	       "AND (:starNumber IS NULL OR v.starNumber = :starNumber) " +
	    	       "AND (:description IS NULL OR LOWER(TRIM(a.description)) IN :description) " +
	    	       "AND (:categoryName IS NULL OR LOWER(TRIM(cc.casteCategoryName)) IN :categoryName) " +
	    	       "AND (:pollStatus IS NULL OR " +
	    	       "     (:pollStatus = 'voted' AND v.hasVoted = true) OR " +
	    	       "     (:pollStatus = 'notVoted' AND (v.hasVoted = false OR v.hasVoted IS NULL)) OR " +
	    	       "     (:pollStatus = 'all')) " +
	    	       "AND (:isFamily IS NULL OR (:isFamily = true AND v.familyId IS NOT NULL) OR (:isFamily = false AND v.familyId IS NULL))")
	    	
	    	Page<VoterEntity> findByAccountIdAndElectionIdAndFiltersOptimized(
	    	    @Param("accountId") Long accountId,
	    	    @Param("electionId") Long electionId,
	    	    @Param("voterId") String voterId,
	    	    @Param("epicNumber") String epicNumber,
	    	    @Param("boothNumbers") List<Integer> boothNumbers,
	    	    @Param("familyId") UUID familyId,
	    	    @Param("friendId") UUID friendId,
	    	    @Param("voterFnameEn") List<String> voterFnameEn,
	    	    @Param("voterLnameEn") List<String> voterLnameEn,
	    	    @Param("voterFnameL1") List<String> voterFnameL1,
	    	    @Param("voterFnameL2") List<String> voterFnameL2,
	    	    @Param("relationFirstNameEn") List<String> relationFirstNameEn,
	    	    @Param("relationLastNameEn") List<String> relationLastNameEn,
	    	    @Param("partyName") List<String> partyName,
	    	    @Param("religionName") List<String> religionName,
	    	    @Param("age") Integer age,
	    	    @Param("minAge") Integer minAge,
	    	    @Param("maxAge") Integer maxAge,
	    	    @Param("includeUnknownAge") Boolean includeUnknownAge,
	    	    @Param("genders") List<String> genders,
	    	    @Param("hasDob") Boolean hasDob,
	    	    @Param("tomorrowMonth") Integer tomorrowMonth,
	    	    @Param("tomorrowDay") Integer tomorrowDay,
	    	    @Param("starNumber") Boolean starNumber,
	    	    @Param("description") List<String> description,
	    	    @Param("categoryName") List<String> categoryName,
	    	    @Param("pollStatus") String pollStatus,
	    	    @Param("isFamily") Boolean isFamily,
	    	    Pageable pageable);
	    
	    @Query("SELECT v FROM VoterEntity v " +
	    	       "WHERE v.accountId = :accountId " +
	    	       "AND v.electionId = :electionId " +
	    	       "AND EXISTS (SELECT 1 FROM VoterEntity v2 " +
	    	       "            WHERE v2.accountId = v.accountId " +
	    	       "            AND v2.electionId = v.electionId " +
	    	       "            AND v2.id <> v.id " +
	    	       "            AND LOWER(TRIM(v2.voterFnameEn)) = LOWER(TRIM(v.voterFnameEn)) " +
	    	       "            AND LOWER(TRIM(v2.voterLnameEn)) = LOWER(TRIM(v.voterLnameEn)) " +
	    	       "            AND LOWER(TRIM(v2.rlnFnameEn)) = LOWER(TRIM(v.rlnFnameEn)) " +
	    	       "            AND LOWER(TRIM(v2.rlnLnameEn)) = LOWER(TRIM(v.rlnLnameEn)) " +
	    	       "            AND v2.age = v.age)")
	    	Page<VoterEntity> findPotentialDuplicates(
	    	    @Param("accountId") Long accountId,
	    	    @Param("electionId") Long electionId,
	    	    Pageable pageable);
			
	// Migration-specific method with eager loading to prevent lazy loading issues
	@Query("SELECT v FROM VoterEntity v " +
	       "LEFT JOIN FETCH v.religion " +
	       "LEFT JOIN FETCH v.caste " +
	       "LEFT JOIN FETCH v.subCaste " +
	       "LEFT JOIN FETCH v.casteCategory " +
	       "LEFT JOIN FETCH v.availability1 " +
	       "LEFT JOIN FETCH v.party " +
	       "LEFT JOIN FETCH v.partManager " +
	       "LEFT JOIN FETCH v.dynamicFields " +
	       "WHERE v.accountId = :accountId AND v.electionId = :electionId " +
	       "ORDER BY v.partNo, v.serialNo")
	Page<VoterEntity> findByAccountIdAndElectionIdWithEagerLoading(@Param("accountId") Long accountId, 
	                                                               @Param("electionId") Long electionId, 
	                                                               Pageable pageable);
		  	    @Query("SELECT v.houseNoEn, COUNT(v) FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId GROUP BY v.houseNoEn")
	    List<Object[]> findDistinctHouseNumbers(@Param("accountId") Long accountId, @Param("electionId") Long electionId);

		    @Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND LOWER(TRIM(v.houseNoEn)) = :houseNoEn")
		    List<VoterEntity> findByAccountIdAndElectionIdAndHouseNoEn(
		            @Param("accountId") Long accountId,
		            @Param("electionId") Long electionId,
		            @Param("houseNoEn") String houseNoEn);
		    
	List<VoterEntity> findAllByFamilyIdAndElectionIdAndAccountId(UUID familyId, Long electionId, Long accountId);

	List<VoterEntity> findByFamilyIdAndElectionIdAndAccountId(UUID familyId, Long electionId, Long accountId);
    
	// Family mapping migration queries
	@Query("SELECT DISTINCT v.electionId FROM VoterEntity v WHERE v.accountId = :accountId AND v.familyId IS NOT NULL")
	List<Object[]> findDistinctElectionIdsWithFamilyMappings(@Param("accountId") Long accountId);
	
	// Bulk migration query for finding all voters with family mappings
	@Query("SELECT v FROM VoterEntity v WHERE v.familyId IS NOT NULL ORDER BY v.accountId, v.electionId, v.id")
	Page<VoterEntity> findVotersWithFamilyMappings(Pageable pageable);
	
	// Scheduler query for finding recently modified voters with family mappings
	@Query("SELECT v FROM VoterEntity v WHERE v.familyId IS NOT NULL AND " +
	       "(v.modifiedTime >= :cutoffTime OR v.createdTime >= :cutoffTime) " +
	       "ORDER BY v.modifiedTime DESC, v.createdTime DESC")
	Page<VoterEntity> findRecentlyModifiedVotersWithFamilyMappings(@Param("cutoffTime") LocalDateTime cutoffTime, 
	                                                               Pageable pageable);
	
	// Count queries for family mapping statistics
	@Query("SELECT COUNT(v) FROM VoterEntity v WHERE v.familyId IS NOT NULL")
	long countVotersWithFamilyMappings();
	
	// Part voter statistics methods (required by existing VoterServiceImpl)
	boolean existsByAccountIdAndElectionIdAndPartNo(Long accountId, Long electionId, Integer partNo);
	
	@Query("SELECT " +
	       "CAST(SUM(CASE WHEN LOWER(v.gender) = 'male' THEN 1 ELSE 0 END) AS long) as maleCount, " +
	       "CAST(SUM(CASE WHEN LOWER(v.gender) = 'female' THEN 1 ELSE 0 END) AS long) as femaleCount, " +
	       "CAST(SUM(CASE WHEN LOWER(v.gender) NOT IN ('male', 'female') THEN 1 ELSE 0 END) AS long) as otherCount, " +
	       "CAST(COUNT(v) AS long) as totalCount, " +
	       "CAST(SUM(CASE WHEN v.hasVoted = true THEN 1 ELSE 0 END) AS long) as votedCount " +
	       "FROM VoterEntity v " +
	       "WHERE v.accountId = :accountId AND v.electionId = :electionId AND v.partNo = :partNo")
	PartVoterStatsProjection getPartVoterStats(@Param("accountId") Long accountId, 
	                                           @Param("electionId") Long electionId, 
	                                           @Param("partNo") Integer partNo);
	
	// Projection interface for part voter statistics (required by existing VoterServiceImpl)
	interface PartVoterStatsProjection {
		long getMaleCount();
		long getFemaleCount();
		long getOtherCount();
		long getTotalCount();
		long getVotedCount();
	}
	
	// Missing methods required by existing VoterServiceImpl and other classes
	// These are temporary stub implementations to fix compilation errors

	// Methods needed for duplicate persisted runs
	@Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId ORDER BY v.id ASC")
	org.springframework.data.domain.Slice<VoterEntity> findByAccountIdAndElectionIdOrderByIdAsc(@Param("accountId") Long accountId,
															   @Param("electionId") Long electionId,
															   Pageable pageable);

	@Query("SELECT v.epicNumber, v.id FROM VoterEntity v WHERE v.electionId = :electionId AND v.accountId = :accountId AND v.epicNumber IN :epicNumbers")
	List<Object[]> findIdsByEpicNumbersAndElectionIdAndAccountId(@Param("epicNumbers") List<String> epicNumbers,
																 @Param("electionId") Long electionId,
																 @Param("accountId") Long accountId);
	
//	@Query(value = """
//		SELECT vbs.voter_id, bs.id, bs.scheme_name, bs.image_url, bs.scheme_by, 
//		       bs.account_id, bs.election_id, bs.order_index
//		FROM voter_benefit_schemes vbs
//		JOIN benefit_schemes bs ON vbs.benefit_scheme_id = bs.id
//		WHERE vbs.voter_id IN :voterIds 
//		  AND bs.account_id = :accountId 
//		  AND bs.election_id = :electionId
//		ORDER BY vbs.voter_id, bs.order_index
//		""", nativeQuery = true)
//	List<Object[]> findBenefitSchemesByVoterIds(@Param("voterIds") List<Long> voterIds, 
//	                                           @Param("accountId") Long accountId, 
//	                                           @Param("electionId") Long electionId);
//	@Query(value = """
//		    SELECT vbs.voter_id, bs.id, bs.scheme_name, bs.image_url, bs.scheme_by, 
//		           bs.account_id, bs.election_id, bs.order_index, bs.user_selection
//		    FROM voter_benefit_schemes vbs
//		    JOIN benefit_schemes bs ON vbs.benefit_scheme_id = bs.id
//		    WHERE vbs.voter_id IN :voterIds 
//		      AND bs.account_id = :accountId 
//		      AND bs.election_id = :electionId
//		    ORDER BY vbs.voter_id, bs.order_index
//		    """, nativeQuery = true)
//		List<Object[]> findBenefitSchemesByVoterIds(@Param("voterIds") List<Long> voterIds, 
//		                                           @Param("accountId") Long accountId, 
//		                                           @Param("electionId") Long electionId);
	@Query(value = """
	        SELECT vbs.voter_id, bs.id, bs.scheme_name, bs.scheme_value, bs.image_url, bs.scheme_by, 
	               bs.account_id, bs.election_id, bs.order_index, bs.user_selection,
	               vbs.selected
	        FROM voter_benefit_schemes vbs
	        JOIN benefit_schemes bs ON vbs.benefit_scheme_id = bs.id
	        WHERE vbs.voter_id IN :voterIds 
	          AND bs.account_id = :accountId 
	          AND bs.election_id = :electionId
	        ORDER BY vbs.voter_id, bs.order_index
	        """, nativeQuery = true)
	List<Object[]> findBenefitSchemesByVoterIds(@Param("voterIds") List<Long> voterIds, 
	                                           @Param("accountId") Long accountId, 
	                                           @Param("electionId") Long electionId);

	
	@Query(value = """
		SELECT vfi.voter_id, fi.id, fi.issue_name, fi.election_id, fi.account_id, fi.order_index
		FROM voter_feedback_issues vfi
		JOIN feedback_issues fi ON vfi.feedback_issue_id = fi.id
		WHERE vfi.voter_id IN :voterIds 
		  AND fi.account_id = :accountId 
		  AND fi.election_id = :electionId
		ORDER BY vfi.voter_id, fi.order_index
		""", nativeQuery = true)
	List<Object[]> findFeedbackIssuesByVoterIds(@Param("voterIds") List<Long> voterIds, 
	                                           @Param("accountId") Long accountId, 
	                                           @Param("electionId") Long electionId);
	
	@Query(value = """
		SELECT vvh.voter_id, vh.id, vh.voter_history_name, vh.voter_history_image, 
		       vh.account_id, vh.election_id, vh.order_index
		FROM voter_voter_history vvh
		JOIN voter_history vh ON vvh.voter_history_id = vh.id
		WHERE vvh.voter_id IN :voterIds 
		  AND vh.account_id = :accountId 
		  AND vh.election_id = :electionId
		ORDER BY vvh.voter_id, vh.order_index
		""", nativeQuery = true)
	List<Object[]> findVoterHistoriesByVoterIds(@Param("voterIds") List<Long> voterIds, 
	                                           @Param("accountId") Long accountId, 
	                                           @Param("electionId") Long electionId);
	
	@Query(value = """
		SELECT vl.voter_id, l.id, l.language_name
		FROM voter_language vl
		JOIN language l ON vl.language_id = l.id
		WHERE vl.voter_id IN :voterIds
		ORDER BY vl.voter_id, l.id
		""", nativeQuery = true)
	List<Object[]> findLanguagesByVoterIds(@Param("voterIds") List<Long> voterIds);
	
	default com.thedal.thedal_app.voter.dto.GenderStatsProjection getFilteredGenderStatsOptimized(Long accountId, Long electionId, String voterFnameEn, 
			String partyName, List<Integer> boothNumbers, java.util.UUID familyId, java.util.UUID partId, 
			List<String> genders, List<String> ages, List<String> religions, List<String> castes, 
			List<String> categories, List<String> languages, List<String> occupations, List<String> educations, 
			String searchTerm, Integer minAge, Integer maxAge, Integer age, Boolean hasDob, 
			List<String> religionNames, Boolean hasMobileNumber, Integer minFamilySize, Integer maxFamilySize, 
			Boolean hasAadhaar, String sortBy, String sortDirection) {
		return null;
	}
	
	default java.util.Optional<VoterEntity> findByAadhaarNumber(String aadhaarNumber) {
		return java.util.Optional.empty();
	}
	
	default java.util.Optional<VoterEntity> findByMobileNoAndElectionIdAndAccountId(String mobileNo, Long electionId, Long accountId) {
		return java.util.Optional.empty();
	}
	
	@Query("""
		SELECT new com.thedal.thedal_app.voter.dto.VoterSearchResultDTO(
			v.voterId, v.voterFnameEn, v.voterFnameL1, v.voterFnameL2, v.voterLnameEn, 
			v.epicNumber, v.rlnFnameEn, v.rlnLnameEn) 
		FROM VoterEntity v 
		WHERE v.accountId = :accountId 
		AND v.electionId = :electionId 
		AND (:boothNumbers IS NULL OR v.boothNumber IN :boothNumbers)
		AND (LOWER(v.voterFnameEn) LIKE LOWER(CONCAT('%', :name, '%')) 
		     OR LOWER(v.voterLnameEn) LIKE LOWER(CONCAT('%', :name, '%'))
		     OR LOWER(v.voterFnameL1) LIKE LOWER(CONCAT('%', :name, '%'))
		     OR LOWER(v.voterFnameL2) LIKE LOWER(CONCAT('%', :name, '%'))
		     OR LOWER(v.voterId) LIKE LOWER(CONCAT('%', :name, '%'))
		     OR LOWER(v.epicNumber) LIKE LOWER(CONCAT('%', :name, '%')))
		""")
	List<com.thedal.thedal_app.voter.dto.VoterSearchResultDTO> findByAccountIdAndElectionIdAndNameLike(
		@Param("accountId") Long accountId, 
		@Param("electionId") Long electionId, 
		@Param("name") String name, 
		@Param("boothNumbers") List<Integer> boothNumbers);
	
	@Query("""
		SELECT new com.thedal.thedal_app.voter.dto.VoterSearchResultDTO(
			v.voterId, v.voterFnameEn, v.voterFnameL1, v.voterFnameL2, v.voterLnameEn, 
			v.epicNumber, v.rlnFnameEn, v.rlnLnameEn) 
		FROM VoterEntity v 
		WHERE v.accountId = :accountId 
		AND v.electionId = :electionId 
		AND (LOWER(v.voterFnameEn) LIKE LOWER(CONCAT('%', :name, '%')) 
		     OR LOWER(v.voterLnameEn) LIKE LOWER(CONCAT('%', :name, '%'))
		     OR LOWER(v.voterFnameL1) LIKE LOWER(CONCAT('%', :name, '%'))
		     OR LOWER(v.voterFnameL2) LIKE LOWER(CONCAT('%', :name, '%'))
		     OR LOWER(v.voterId) LIKE LOWER(CONCAT('%', :name, '%'))
		     OR LOWER(v.epicNumber) LIKE LOWER(CONCAT('%', :name, '%')))
		""")
	List<com.thedal.thedal_app.voter.dto.VoterSearchResultDTO> findByAccountIdAndElectionIdAndNameLike(
		@Param("accountId") Long accountId, 
		@Param("electionId") Long electionId, 
		@Param("name") String name);

	// Enhanced name search with concatenated field matching for initials (e.g., "M Anandan")
	@Query("""
		SELECT new com.thedal.thedal_app.voter.dto.VoterSearchResultDTO(
			v.voterId, v.voterFnameEn, v.voterFnameL1, v.voterFnameL2, v.voterLnameEn, 
			v.epicNumber, v.rlnFnameEn, v.rlnLnameEn) 
		FROM VoterEntity v 
		WHERE v.accountId = :accountId 
		AND v.electionId = :electionId 
		AND (LOWER(TRIM(v.voterFnameEn)) LIKE LOWER(CONCAT('%', TRIM(:name), '%')) 
		     OR LOWER(TRIM(v.voterLnameEn)) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(TRIM(v.voterFnameL1)) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(TRIM(v.voterFnameL2)) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(CONCAT(TRIM(v.voterFnameEn), ' ', TRIM(v.voterLnameEn))) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(CONCAT(TRIM(v.voterLnameEn), ' ', TRIM(v.voterFnameEn))) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(CONCAT(TRIM(v.voterFnameL1), ' ', TRIM(v.voterLnameEn))) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(CONCAT(TRIM(v.voterFnameL2), ' ', TRIM(v.voterLnameEn))) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(TRIM(v.voterId)) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(TRIM(v.epicNumber)) LIKE LOWER(CONCAT('%', TRIM(:name), '%')))
		AND (:isFamily IS NULL OR (:isFamily = true AND v.familyId IS NOT NULL) OR (:isFamily = false AND v.familyId IS NULL))
		""")
	List<com.thedal.thedal_app.voter.dto.VoterSearchResultDTO> findByAccountIdAndElectionIdAndNameLikeEnhanced(
		@Param("accountId") Long accountId, 
		@Param("electionId") Long electionId, 
		@Param("name") String name, 
		@Param("isFamily") Boolean isFamily);

	// Enhanced name search with booth restriction for volunteers
	@Query("""
		SELECT new com.thedal.thedal_app.voter.dto.VoterSearchResultDTO(
			v.voterId, v.voterFnameEn, v.voterFnameL1, v.voterFnameL2, v.voterLnameEn, 
			v.epicNumber, v.rlnFnameEn, v.rlnLnameEn) 
		FROM VoterEntity v 
		WHERE v.accountId = :accountId 
		AND v.electionId = :electionId 
		AND v.boothNumber IN :boothNumbers
		AND (LOWER(TRIM(v.voterFnameEn)) LIKE LOWER(CONCAT('%', TRIM(:name), '%')) 
		     OR LOWER(TRIM(v.voterLnameEn)) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(TRIM(v.voterFnameL1)) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(TRIM(v.voterFnameL2)) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(CONCAT(TRIM(v.voterFnameEn), ' ', TRIM(v.voterLnameEn))) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(CONCAT(TRIM(v.voterLnameEn), ' ', TRIM(v.voterFnameEn))) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(CONCAT(TRIM(v.voterFnameL1), ' ', TRIM(v.voterLnameEn))) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(CONCAT(TRIM(v.voterFnameL2), ' ', TRIM(v.voterLnameEn))) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(TRIM(v.voterId)) LIKE LOWER(CONCAT('%', TRIM(:name), '%'))
		     OR LOWER(TRIM(v.epicNumber)) LIKE LOWER(CONCAT('%', TRIM(:name), '%')))
		AND (:isFamily IS NULL OR (:isFamily = true AND v.familyId IS NOT NULL) OR (:isFamily = false AND v.familyId IS NULL))
		""")
	List<com.thedal.thedal_app.voter.dto.VoterSearchResultDTO> findByAccountIdAndElectionIdAndNameLikeEnhanced(
		@Param("accountId") Long accountId, 
		@Param("electionId") Long electionId, 
		@Param("name") String name, 
		@Param("boothNumbers") List<Integer> boothNumbers, 
		@Param("isFamily") Boolean isFamily);
	
	@Query("SELECT v FROM VoterEntity v " +
	       "LEFT JOIN FETCH v.caste " +
	       "LEFT JOIN FETCH v.subCaste " +
	       "LEFT JOIN FETCH v.religion " +
	       "LEFT JOIN FETCH v.party " +
	       "LEFT JOIN FETCH v.availability1 " +
	       "WHERE v.accountId = :accountId AND v.electionId = :electionId AND v.voterId IN :voterIds ORDER BY v.partNo, v.serialNo")
	org.springframework.data.domain.Page<VoterEntity> findByAccountIdAndElectionIdAndVoterIdIn(
		@Param("accountId") Long accountId, 
		@Param("electionId") Long electionId, 
		@Param("voterIds") List<String> voterIds, 
		org.springframework.data.domain.Pageable pageable);
	
	@Query("SELECT " +
		   "v.boothNumber as boothNumber, " +
		   "COALESCE(SUM(CASE WHEN v.aadhaarVerified = true THEN 1 ELSE 0 END), 0) as verifiedCount, " +
		   "COALESCE(SUM(CASE WHEN v.aadhaarVerified = false OR v.aadhaarVerified IS NULL THEN 1 ELSE 0 END), 0) as unverifiedCount, " +
		   "COALESCE(COUNT(v), 0) as totalCount " +
		   "FROM VoterEntity v " +
		   "WHERE v.accountId = :accountId " +
		   "AND v.electionId = :electionId " +
		   "AND v.boothNumber IN :boothNumbers " +
		   "GROUP BY v.boothNumber")
	List<com.thedal.thedal_app.voter.dto.BoothAadhaarStatsProjection> getBoothAadhaarStats(Long accountId, Long electionId, List<Integer> boothNumbers);
	
	@Query("SELECT " +
		   "v.boothNumber as boothNumber, " +
		   "COALESCE(SUM(CASE WHEN v.memberVerified = true THEN 1 ELSE 0 END), 0) as verifiedCount, " +
		   "COALESCE(SUM(CASE WHEN v.memberVerified = false OR v.memberVerified IS NULL THEN 1 ELSE 0 END), 0) as unverifiedCount, " +
		   "COALESCE(COUNT(v), 0) as totalCount " +
		   "FROM VoterEntity v " +
		   "WHERE v.accountId = :accountId " +
		   "AND v.electionId = :electionId " +
		   "AND v.boothNumber IN :boothNumbers " +
		   "GROUP BY v.boothNumber")
	List<com.thedal.thedal_app.voter.dto.BoothMembershipStatsProjection> getBoothMembershipStats(Long accountId, Long electionId, List<Integer> boothNumbers);
	
	@Query("SELECT " +
		   "COALESCE(SUM(CASE WHEN v.aadhaarVerified = true THEN 1 ELSE 0 END), 0) as verifiedCount, " +
		   "COALESCE(SUM(CASE WHEN v.aadhaarVerified = false OR v.aadhaarVerified IS NULL THEN 1 ELSE 0 END), 0) as unverifiedCount, " +
		   "COALESCE(COUNT(v), 0) as totalCount " +
		   "FROM VoterEntity v " +
		   "WHERE v.accountId = :accountId " +
		   "AND v.electionId = :electionId")
	com.thedal.thedal_app.voter.dto.AadhaarStatsProjection getAadhaarStats(Long accountId, Long electionId);
	
	@Query("SELECT " +
		   "COALESCE(SUM(CASE WHEN v.memberVerified = true THEN 1 ELSE 0 END), 0) as verifiedCount, " +
		   "COALESCE(SUM(CASE WHEN v.memberVerified = false OR v.memberVerified IS NULL THEN 1 ELSE 0 END), 0) as unverifiedCount, " +
		   "COALESCE(COUNT(v), 0) as totalCount " +
		   "FROM VoterEntity v " +
		   "WHERE v.accountId = :accountId " +
		   "AND v.electionId = :electionId")
	com.thedal.thedal_app.voter.dto.MembershipStatsProjection getMembershipStats(Long accountId, Long electionId);
	
	@Query("SELECT " +
		   "COALESCE(SUM(CASE WHEN v.availability1 IS NOT NULL THEN 1 ELSE 0 END), 0) as addressedCount, " +
		   "COALESCE(SUM(CASE WHEN v.availability1 IS NULL THEN 1 ELSE 0 END), 0) as notAddressedCount, " +
		   "COALESCE(COUNT(v), 0) as totalCount " +
		   "FROM VoterEntity v " +
		   "WHERE v.accountId = :accountId " +
		   "AND v.electionId = :electionId")
	com.thedal.thedal_app.voter.dto.AddressedVoterStatsProjection getAddressedVoterStats(Long accountId, Long electionId);
	
	@Query("SELECT " +
		   "COALESCE(SUM(CASE WHEN v.familyId IS NULL THEN 1 ELSE 0 END), 0) as unmappedVoterCount, " +
		   "COALESCE(COUNT(DISTINCT CASE WHEN v.familyCount = 1 THEN v.familyId END), 0) as singleVoterFamilyCount, " +
		   "COALESCE(COUNT(v), 0) as totalCount " +
		   "FROM VoterEntity v " +
		   "WHERE v.accountId = :accountId " +
		   "AND v.electionId = :electionId " +
		   "AND (:partNumbers IS NULL OR v.partNo IN :partNumbers)")
	com.thedal.thedal_app.voter.dto.FamilyMappingStatsProjection getFamilyMappingStats(
		Long accountId, Long electionId, @Param("partNumbers") List<Integer> partNumbers);
	
	default int countFriendsByVoterId(Long voterId, Long electionId, Long accountId) {
		return 0;
	}
	
    
	
	default boolean existsByElectionIdAndAvailability1IsNotNull(Long electionId) {
		return false;
	}
	
	 @Modifying
	    @Query("DELETE FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND v.epicNumber IN :epicNumbers")
	    int deleteByAccountIdAndElectionIdAndEpicNumberIn(
	        @Param("accountId") Long accountId, 
	        @Param("electionId") Long electionId, 
	        @Param("epicNumbers") List<String> epicNumbers);
	 
	 
	 @Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId " +
		       "AND v.familyId IN (" +
		       "SELECT v2.familyId FROM VoterEntity v2 WHERE v2.accountId = :accountId AND v2.electionId = :electionId " +
		       "AND (:boothNumbers IS NULL OR v2.partNo IN :boothNumbers) " +
		       "AND v2.age = (SELECT MAX(v3.age) FROM VoterEntity v3 WHERE v3.familyId = v2.familyId " +
		       "AND v3.accountId = :accountId AND v3.electionId = :electionId)" +
		       ") AND v.familyId IS NOT NULL")
		Page<VoterEntity> findFamiliesByEldestMemberPartNo(
		        @Param("accountId") Long accountId,
		        @Param("electionId") Long electionId,
		        @Param("boothNumbers") List<Integer> boothNumbers,
		        Pageable pageable);
	 

	 
	@Query("SELECT v FROM VoterEntity v WHERE v.electionId = :electionId AND v.accountId = :accountId " +
	           "AND (:partNo IS NULL OR v.partNo = :partNo) " +
	           "AND (:gender IS NULL OR LOWER(v.gender) = LOWER(:gender)) " +
	           "AND (:ageMin IS NULL OR v.age >= :ageMin) " +
	           "AND (:ageMax IS NULL OR v.age <= :ageMax)")
	    List<VoterEntity> findFilteredVotersForExport(@Param("electionId") Long electionId,
	                                                  @Param("accountId") Long accountId,
	                                                  @Param("partNo") Integer partNo,
	                                                  @Param("gender") String gender,
	                                                  @Param("ageMin") Integer ageMin,
	                                                  @Param("ageMax") Integer ageMax);
	
	/**
	 * Projection-based query for voter export to reduce memory usage
	 */
	Page<VoterExportProjection> findProjectedBy(Specification<VoterEntity> spec, Pageable pageable);
	
	
	@Query("SELECT v FROM VoterEntity v " +
		       "LEFT JOIN FETCH v.availability1 a " +
		       "LEFT JOIN FETCH v.party p " +
		       "LEFT JOIN FETCH v.religion r " +
		       "LEFT JOIN FETCH v.caste c " +
		       "LEFT JOIN FETCH v.subCaste sc " +
		       "LEFT JOIN v.partManager pm " +
		       "WHERE v.accountId = :accountId " +
		       "AND v.electionId = :electionId " +
		       "AND (:friendId IS NULL OR v.friendId = :friendId) " +
		       "AND (:partNumbers IS NULL OR v.partNo IN :partNumbers)")
		Page<VoterEntity> findByAccountIdAndElectionId(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId,
		    @Param("friendId") UUID friendId,
		    @Param("partNumbers") List<Integer> partNumbers,
		    Pageable pageable);

		@Query("SELECT " +
		       "COALESCE(COUNT(DISTINCT CASE WHEN LOWER(TRIM(v.gender)) IN ('male', 'm') THEN v.id END), 0) as maleCount, " +
		       "COALESCE(COUNT(DISTINCT CASE WHEN LOWER(TRIM(v.gender)) IN ('female', 'f') THEN v.id END), 0) as femaleCount, " +
		       "COALESCE(COUNT(DISTINCT CASE WHEN LOWER(TRIM(v.gender)) NOT IN ('male', 'm', 'female', 'f') AND v.gender IS NOT NULL THEN v.id END), 0) as otherCount, " +
		       "COALESCE(COUNT(DISTINCT v), 0) as totalCount " +
		       "FROM VoterEntity v " +
		       "WHERE v.accountId = :accountId " +
		       "AND v.electionId = :electionId " +
		       "AND (v.friendId IS NOT NULL " +
		       "     OR v.epicNumber IN :friendEpicNumbers) " +
		       "AND (:partNumbers IS NULL OR v.partNo IN :partNumbers)")
		GenderStatsProjection getGenderStatsByFriend(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId,
		    @Param("friendEpicNumbers") List<String> friendEpicNumbers,
		    @Param("partNumbers") List<Integer> partNumbers);
	
	
		@Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId AND v.epicNumber IN :epicNumbers")
		List<VoterEntity> findByAccountIdAndElectionIdAndEpicNumbers(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId,
		    @Param("epicNumbers") List<String> epicNumbers
		);
		
		@Query("SELECT COALESCE(COUNT(v), 0) " +
			       "FROM VoterEntity v " +
			       "WHERE v.accountId = :accountId " +
			       "AND v.electionId = :electionId " +
			       "AND (:partNumbers IS NULL OR v.partNo IN :partNumbers)")
			Long countVotersByElectionAndParts(
			        @Param("accountId") Long accountId,
			        @Param("electionId") Long electionId,
			        @Param("partNumbers") List<Integer> partNumbers);
		
		
		 @Query("SELECT v FROM VoterEntity v WHERE v.accountId = :accountId " +
		           "AND v.electionId = :electionId " +
		           "AND v.dob IS NOT NULL " +
		           "AND EXTRACT(MONTH FROM v.dob) = :month " +
		           "AND EXTRACT(DAY FROM v.dob) = :day")
		    List<VoterEntity> findVotersByBirthdayToday(Long accountId, Long electionId, int month, int day, Pageable pageable);
		
	// Family sequence number related queries
	@Query("SELECT COALESCE(MAX(v.familySequenceNumber), 0) " +
	       "FROM VoterEntity v " +
	       "WHERE v.accountId = :accountId AND v.electionId = :electionId " +
	       "AND v.familyId IS NOT NULL")
	Integer getMaxFamilySequenceNumber(
	    @Param("accountId") Long accountId, 
	    @Param("electionId") Long electionId
	);

	@Modifying
	@Query("UPDATE VoterEntity v SET v.familySequenceNumber = :sequenceNumber " +
	       "WHERE v.familyId = :familyId AND v.accountId = :accountId AND v.electionId = :electionId")
	int updateFamilySequenceNumber(
	    @Param("familyId") UUID familyId,
	    @Param("sequenceNumber") Integer sequenceNumber,
	    @Param("accountId") Long accountId,
	    @Param("electionId") Long electionId
	);		// Updated family summary query with sequence numbering and part override support
		@Query(value = """
		    WITH part_filter AS (
		        SELECT unnest(string_to_array(NULLIF(:partNumbersCsv, ''), ','))::BIGINT AS part_no
		    ),
		    cross_family_check AS (
		        SELECT 
		            v.family_id,
		            COUNT(DISTINCT v.part_no) > 1 AS is_cross_family
		        FROM _voters v
		        WHERE v.account_id = :accountId
		          AND v.election_id = :electionId
		          AND v.family_id IS NOT NULL
		        GROUP BY v.family_id
		    ),
		    reps AS (
		        SELECT DISTINCT ON (v.family_id)
		            v.family_id,
		            v.family_sequence_number,
		            v.family_count AS member_count,
		            v.voter_fname_en AS first_member_name,
		            v.epic_number AS first_member_epic,
		            v.age AS first_member_age,
		            v.gender AS first_member_gender,
		            COALESCE(v.family_display_part, v.part_no) AS effective_part,
		            v.part_no AS first_member_part,
		            v.serial_no AS first_member_serial_no,
		            v.mobile_no AS first_member_mobile_no,
		            v.rln_type AS first_member_rln_type,
		            v.voter_fname_en AS first_member_voter_fname_en,
		            v.voter_lname_en AS first_member_voter_lname_en,
		            v.voter_fname_l1 AS first_member_voter_fname_l1,
		            v.voter_lname_l1 AS first_member_voter_lname_l1,
		            v.rln_fname_en AS first_member_rln_fname_en,
		            v.rln_lname_en AS first_member_rln_lname_en,
		            v.rln_fname_l1 AS first_member_rln_fname_l1,
		            v.rln_lname_l1 AS first_member_rln_lname_l1,
					v.member_verified AS first_member_verified,
					v.aadhaar_verified AS first_member_aadhaar_verified,
					v.availability_id AS first_member_availability_id,
					v.party_id AS first_member_party_id,
					v.aadhaar_number AS first_member_aadhaar_number,
					v.pan_number AS first_member_pan_number,
					v.photo_url AS first_member_photo_url,
					a.description AS first_member_availability_name,
					p.party_name AS first_member_party_name,
					v.voter_id AS first_member_voter_id,
					v.id AS first_member_voter_pk
				FROM _voters v
				LEFT JOIN availability a ON a.id = v.availability_id AND a.election_id = v.election_id AND a.account_id = v.account_id
				LEFT JOIN parties p ON p.id = v.party_id AND p.election_id = v.election_id AND p.account_id = v.account_id
				INNER JOIN cross_family_check cfc ON cfc.family_id = v.family_id
		        WHERE v.account_id = :accountId
		          AND v.election_id = :electionId
		          AND v.family_id IS NOT NULL
		          AND (:crossFamily IS NULL OR cfc.is_cross_family = :crossFamily)
		        ORDER BY v.family_id, v.is_family_head DESC NULLS LAST, v.age DESC NULLS LAST, v.voter_id
		    ),
		    voting_history AS (
		        SELECT 
		            vvh.voter_id,
		            COALESCE(
		                json_agg(
		                    json_build_object(
		                        'id', vh.id,
		                        'name', vh.voter_history_name,
		                        'image', vh.voter_history_image,
		                        'orderIndex', vh.order_index
		                    ) ORDER BY vh.order_index
		                ) FILTER (WHERE vh.id IS NOT NULL),
		                '[]'::json
		            ) AS history_json
				FROM reps r
				LEFT JOIN voter_voter_history vvh ON vvh.voter_id = r.first_member_voter_pk
		        LEFT JOIN voter_history vh ON vh.id = vvh.voter_history_id 
		            AND vh.account_id = :accountId 
		            AND vh.election_id = :electionId
		        GROUP BY vvh.voter_id
		    )
			SELECT
				r.family_id,
				r.family_sequence_number,
				r.member_count,
				r.first_member_name,
				r.first_member_epic,
				r.first_member_age,
				r.first_member_gender,
				r.effective_part,
				r.first_member_part,
				r.first_member_serial_no,
				r.first_member_mobile_no,
				r.first_member_rln_type,
				r.first_member_voter_fname_en,
				r.first_member_voter_lname_en,
				r.first_member_voter_fname_l1,
				r.first_member_voter_lname_l1,
				r.first_member_rln_fname_en,
				r.first_member_rln_lname_en,
				r.first_member_rln_fname_l1,
				r.first_member_rln_lname_l1,
				r.first_member_verified,
				r.first_member_aadhaar_verified,
				r.first_member_availability_id,
				r.first_member_party_id,
				r.first_member_aadhaar_number,
				r.first_member_pan_number,
				r.first_member_photo_url,
				r.first_member_availability_name,
				r.first_member_party_name,
				r.first_member_voter_id,
				COALESCE(vh.history_json, '[]'::json) AS voting_history_json
			FROM reps r
			LEFT JOIN voting_history vh ON vh.voter_id = r.first_member_voter_pk
		    ORDER BY 
		        CASE WHEN family_sequence_number IS NOT NULL THEN 0 ELSE 1 END,
		        family_sequence_number ASC NULLS LAST, 
		        family_id
		    """, nativeQuery = true)
		List<Object[]> findFamilySummaryWithSequenceAll(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId,
		    @Param("crossFamily") Boolean crossFamily
		);

		@Query(value = """
		    WITH part_filter AS (
		        SELECT unnest(string_to_array(NULLIF(:partNumbersCsv, ''), ','))::BIGINT AS part_no
		    ),
		    cross_family_check AS (
		        SELECT 
		            v.family_id,
		            COUNT(DISTINCT v.part_no) > 1 AS is_cross_family
		        FROM _voters v
		        WHERE v.account_id = :accountId
		          AND v.election_id = :electionId
		          AND v.family_id IS NOT NULL
		        GROUP BY v.family_id
		    ),
		    family_reps AS (
		        SELECT DISTINCT ON (v.family_id)
		            v.family_id,
		            COALESCE(v.family_display_part, v.part_no) AS rep_effective_part
		        FROM _voters v
		        WHERE v.account_id = :accountId
		          AND v.election_id = :electionId
		          AND v.family_id IS NOT NULL
		        ORDER BY v.family_id, v.is_family_head DESC NULLS LAST, v.age DESC NULLS LAST, v.voter_id
		    ),
		    reps AS (
		        SELECT DISTINCT ON (v.family_id)
		            v.family_id,
		            v.family_sequence_number,
		            v.family_count AS member_count,
		            v.voter_fname_en AS first_member_name,
		            v.epic_number AS first_member_epic,
		            v.age AS first_member_age,
		            v.gender AS first_member_gender,
		            COALESCE(v.family_display_part, v.part_no) AS effective_part,
		            v.part_no AS first_member_part,
		            v.serial_no AS first_member_serial_no,
		            v.mobile_no AS first_member_mobile_no,
		            v.rln_type AS first_member_rln_type,
		            v.voter_fname_en AS first_member_voter_fname_en,
		            v.voter_lname_en AS first_member_voter_lname_en,
		            v.voter_fname_l1 AS first_member_voter_fname_l1,
		            v.voter_lname_l1 AS first_member_voter_lname_l1,
		            v.rln_fname_en AS first_member_rln_fname_en,
		            v.rln_lname_en AS first_member_rln_lname_en,
		            v.rln_fname_l1 AS first_member_rln_fname_l1,
		            v.rln_lname_l1 AS first_member_rln_lname_l1,
					v.member_verified AS first_member_verified,
					v.aadhaar_verified AS first_member_aadhaar_verified,
					v.availability_id AS first_member_availability_id,
					v.party_id AS first_member_party_id,
					v.aadhaar_number AS first_member_aadhaar_number,
					v.pan_number AS first_member_pan_number,
					v.photo_url AS first_member_photo_url,
					a.description AS first_member_availability_name,
					p.party_name AS first_member_party_name,
					v.voter_id AS first_member_voter_id,
					v.id AS first_member_voter_pk
				FROM _voters v
				LEFT JOIN availability a ON a.id = v.availability_id AND a.election_id = v.election_id AND a.account_id = v.account_id
				LEFT JOIN parties p ON p.id = v.party_id AND p.election_id = v.election_id AND p.account_id = v.account_id
				INNER JOIN family_reps fr ON fr.family_id = v.family_id 
				    AND (:partNumbersCsv IS NULL OR CAST(fr.rep_effective_part AS BIGINT) IN (SELECT part_no FROM part_filter))
				INNER JOIN cross_family_check cfc ON cfc.family_id = v.family_id
		        WHERE v.account_id = :accountId
		          AND v.election_id = :electionId
		          AND v.family_id IS NOT NULL
		          AND (:crossFamily IS NULL OR cfc.is_cross_family = :crossFamily)
		        ORDER BY v.family_id, v.is_family_head DESC NULLS LAST, v.age DESC NULLS LAST, v.voter_id
		    ),
		    voting_history AS (
		        SELECT 
		            vvh.voter_id,
		            COALESCE(
		                json_agg(
		                    json_build_object(
		                        'id', vh.id,
		                        'name', vh.voter_history_name,
		                        'image', vh.voter_history_image,
		                        'orderIndex', vh.order_index
		                    ) ORDER BY vh.order_index
		                ) FILTER (WHERE vh.id IS NOT NULL),
		                '[]'::json
		            ) AS history_json
				FROM reps r
				LEFT JOIN voter_voter_history vvh ON vvh.voter_id = r.first_member_voter_pk
		        LEFT JOIN voter_history vh ON vh.id = vvh.voter_history_id 
		            AND vh.account_id = :accountId 
		            AND vh.election_id = :electionId
		        GROUP BY vvh.voter_id
		    )
			SELECT
				r.family_id,
				r.family_sequence_number,
				r.member_count,
				r.first_member_name,
				r.first_member_epic,
				r.first_member_age,
				r.first_member_gender,
				r.effective_part,
				r.first_member_part,
				r.first_member_serial_no,
				r.first_member_mobile_no,
				r.first_member_rln_type,
				r.first_member_voter_fname_en,
				r.first_member_voter_lname_en,
				r.first_member_voter_fname_l1,
				r.first_member_voter_lname_l1,
				r.first_member_rln_fname_en,
				r.first_member_rln_lname_en,
				r.first_member_rln_fname_l1,
				r.first_member_rln_lname_l1,
				r.first_member_verified,
				r.first_member_aadhaar_verified,
				r.first_member_availability_id,
				r.first_member_party_id,
				r.first_member_aadhaar_number,
				r.first_member_pan_number,
				r.first_member_photo_url,
				r.first_member_availability_name,
				r.first_member_party_name,
				r.first_member_voter_id,
				COALESCE(vh.history_json, '[]'::json) AS voting_history_json
			FROM reps r
			LEFT JOIN voting_history vh ON vh.voter_id = r.first_member_voter_pk
		    ORDER BY 
		        CASE WHEN family_sequence_number IS NOT NULL THEN 0 ELSE 1 END,
		        family_sequence_number ASC NULLS LAST, 
		        family_id
		    """, nativeQuery = true)
		List<Object[]> findFamilySummaryWithSequenceByParts(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId,
		    @Param("partNumbersCsv") String partNumbersCsv,
		    @Param("crossFamily") Boolean crossFamily
		);

		@Query(value = """
		    WITH cross_family_check AS (
		        SELECT 
		            v.family_id,
		            COUNT(DISTINCT v.part_no) > 1 AS is_cross_family
		        FROM _voters v
		        WHERE v.account_id = :accountId
		          AND v.election_id = :electionId
		          AND v.family_id IS NOT NULL
		        GROUP BY v.family_id
		    ),
		    family_reps AS (
		        SELECT DISTINCT ON (v.family_id)
		            v.family_id,
		            COALESCE(v.family_display_part, v.part_no) AS rep_effective_part,
		            v.voter_fname_en AS rep_name
		        FROM _voters v
		        WHERE v.account_id = :accountId
		          AND v.election_id = :electionId
		          AND v.family_id IS NOT NULL
		        ORDER BY v.family_id, v.is_family_head DESC NULLS LAST, v.age DESC NULLS LAST, v.voter_id
		    ),
		    reps AS (
		        SELECT DISTINCT ON (v.family_id)
		            v.family_id,
		            v.family_sequence_number,
		            v.family_count AS member_count,
		            v.voter_fname_en AS first_member_name,
		            v.epic_number AS first_member_epic,
		            v.age AS first_member_age,
		            v.gender AS first_member_gender,
		            COALESCE(v.family_display_part, v.part_no) AS effective_part,
		            v.part_no AS first_member_part,
		            v.serial_no AS first_member_serial_no,
		            v.mobile_no AS first_member_mobile_no,
		            v.rln_type AS first_member_rln_type,
		            v.voter_fname_en AS first_member_voter_fname_en,
		            v.voter_lname_en AS first_member_voter_lname_en,
		            v.voter_fname_l1 AS first_member_voter_fname_l1,
		            v.voter_lname_l1 AS first_member_voter_lname_l1,
		            v.rln_fname_en AS first_member_rln_fname_en,
		            v.rln_lname_en AS first_member_rln_lname_en,
		            v.rln_fname_l1 AS first_member_rln_fname_l1,
		            v.rln_lname_l1 AS first_member_rln_lname_l1,
					v.member_verified AS first_member_verified,
					v.aadhaar_verified AS first_member_aadhaar_verified,
					v.availability_id AS first_member_availability_id,
					v.party_id AS first_member_party_id,
					v.aadhaar_number AS first_member_aadhaar_number,
					v.pan_number AS first_member_pan_number,
					v.photo_url AS first_member_photo_url,
					a.description AS first_member_availability_name,
					p.party_name AS first_member_party_name,
					v.voter_id AS first_member_voter_id,
					v.id AS first_member_voter_pk
		        FROM _voters v
		        LEFT JOIN availability a ON a.id = v.availability_id AND a.election_id = v.election_id AND a.account_id = v.account_id
		        LEFT JOIN parties p ON p.id = v.party_id AND p.election_id = v.election_id AND p.account_id = v.account_id
		        INNER JOIN family_reps fr ON fr.family_id = v.family_id 
		          AND (:partNumbersCsv IS NULL OR CAST(fr.rep_effective_part AS BIGINT) IN (SELECT part_no FROM part_filter))
		          AND (:nameFilter IS NULL OR LOWER(fr.rep_name) LIKE :nameFilter)
		        INNER JOIN cross_family_check cfc ON cfc.family_id = v.family_id
		        WHERE v.account_id = :accountId
		          AND v.election_id = :electionId
		          AND v.family_id IS NOT NULL
		          AND (:crossFamily IS NULL OR cfc.is_cross_family = :crossFamily)
		        ORDER BY v.family_id, v.is_family_head DESC NULLS LAST, v.age DESC NULLS LAST, v.voter_id
		    ),
		    voting_history AS (
		        SELECT 
		            vvh.voter_id,
		            COALESCE(
		                json_agg(
		                    json_build_object(
		                        'id', vh.id,
		                        'name', vh.voter_history_name,
		                        'image', vh.voter_history_image,
		                        'orderIndex', vh.order_index
		                    ) ORDER BY vh.order_index
		                ) FILTER (WHERE vh.id IS NOT NULL),
		                '[]'::json
		            ) AS history_json
				FROM reps r
				LEFT JOIN voter_voter_history vvh ON vvh.voter_id = r.first_member_voter_pk
		        LEFT JOIN voter_history vh ON vh.id = vvh.voter_history_id 
		            AND vh.account_id = :accountId 
		            AND vh.election_id = :electionId
		        GROUP BY vvh.voter_id
		    )
			SELECT
				r.family_id,
				r.family_sequence_number,
				r.member_count,
				r.first_member_name,
				r.first_member_epic,
				r.first_member_age,
				r.first_member_gender,
				r.effective_part,
				r.first_member_part,
				r.first_member_serial_no,
				r.first_member_mobile_no,
				r.first_member_rln_type,
				r.first_member_voter_fname_en,
				r.first_member_voter_lname_en,
				r.first_member_voter_fname_l1,
				r.first_member_voter_lname_l1,
				r.first_member_rln_fname_en,
				r.first_member_rln_lname_en,
				r.first_member_rln_fname_l1,
				r.first_member_rln_lname_l1,
				r.first_member_verified,
				r.first_member_aadhaar_verified,
				r.first_member_availability_id,
				r.first_member_party_id,
				r.first_member_aadhaar_number,
				r.first_member_pan_number,
				r.first_member_photo_url,
				r.first_member_availability_name,
				r.first_member_party_name,
				r.first_member_voter_id,
				COALESCE(vh.history_json, '[]'::json) AS voting_history_json
			FROM reps r
			LEFT JOIN voting_history vh ON vh.voter_id = r.first_member_voter_pk
		    ORDER BY 
		        CASE WHEN family_sequence_number IS NOT NULL THEN 0 ELSE 1 END,
		        family_sequence_number ASC NULLS LAST, 
		        family_id
		    """, nativeQuery = true)
		List<Object[]> findFamilySummaryWithSequenceByName(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId,
		    @Param("partNumbersCsv") String partNumbersCsv,
		    @Param("nameFilter") String nameFilter,
		    @Param("crossFamily") Boolean crossFamily
		);

        // Basic gender stats for entire election/account (used by VoterServiceImpl)
        @Query("SELECT " +
               "COALESCE(SUM(CASE WHEN LOWER(TRIM(v.gender)) IN ('male','m') THEN 1 ELSE 0 END), 0) as maleCount, " +
               "COALESCE(SUM(CASE WHEN LOWER(TRIM(v.gender)) IN ('female','f') THEN 1 ELSE 0 END), 0) as femaleCount, " +
               "COALESCE(SUM(CASE WHEN LOWER(TRIM(v.gender)) NOT IN ('male','m','female','f') AND v.gender IS NOT NULL THEN 1 ELSE 0 END), 0) as otherCount, " +
               "COALESCE(COUNT(v), 0) as totalCount " +
               "FROM VoterEntity v " +
               "WHERE v.accountId = :accountId " +
               "AND v.electionId = :electionId")
        GenderStatsProjection getGenderStats(@Param("accountId") Long accountId,
                                            @Param("electionId") Long electionId);

		// Family sequence number management queries
		@Query("SELECT COUNT(v) FROM VoterEntity v WHERE v.accountId = :accountId " +
		       "AND v.electionId = :electionId AND v.familySequenceNumber = :sequenceNumber " +
		       "AND v.familyId != :familyId")
		long countByAccountIdAndElectionIdAndFamilySequenceNumberAndFamilyIdNot(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId,
		    @Param("sequenceNumber") Integer sequenceNumber,
		    @Param("familyId") UUID familyId
		);

		@Query("SELECT DISTINCT v.familyId FROM VoterEntity v WHERE v.accountId = :accountId " +
		       "AND v.electionId = :electionId AND v.familyId IS NOT NULL " +
		       "ORDER BY v.familyId")
		List<UUID> findDistinctFamilyIdsByElection(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId
		);
		
		// Fast count query for family existence check
		@Query(value = """
		    SELECT COUNT(DISTINCT family_id) 
		    FROM _voters 
		    WHERE account_id = :accountId 
		      AND election_id = :electionId 
		      AND family_id IS NOT NULL
		      AND family_count > 1
		      AND (:partNumbers IS NULL OR COALESCE(family_display_part, part_no) IN (:partNumbers))
		    """, nativeQuery = true)
		Long countDistinctFamiliesForElection(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId,
		    @Param("partNumbers") List<Integer> partNumbers
		);
		
		// Family part override management queries
		@Modifying
		@Query("UPDATE VoterEntity v SET v.familyDisplayPart = :partNumber " +
		       "WHERE v.accountId = :accountId AND v.electionId = :electionId " +
		       "AND v.familyId = :familyId")
		int setFamilyPartOverride(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId,
		    @Param("familyId") UUID familyId,
		    @Param("partNumber") Integer partNumber
		);
		
		@Modifying
		@Query("UPDATE VoterEntity v SET v.familyDisplayPart = NULL " +
		       "WHERE v.accountId = :accountId AND v.electionId = :electionId " +
		       "AND v.familyId = :familyId")
		int removeFamilyPartOverride(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId,
		    @Param("familyId") UUID familyId
		);
		
		// Family head management queries
		@Modifying
		@Query("UPDATE VoterEntity v SET v.isFamilyHead = false " +
		       "WHERE v.accountId = :accountId AND v.electionId = :electionId " +
		       "AND v.familyId = :familyId")
		int clearAllFamilyHeads(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId,
		    @Param("familyId") UUID familyId
		);
		
		@Modifying
		@Query("UPDATE VoterEntity v SET v.isFamilyHead = CASE " +
		       "WHEN v.id = :voterId THEN true ELSE false END " +
		       "WHERE v.accountId = :accountId AND v.electionId = :electionId " +
		       "AND v.familyId = :familyId")
		int setFamilyHead(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId,
		    @Param("familyId") UUID familyId,
		    @Param("voterId") Long voterId
		);
		
		@Query("SELECT COUNT(DISTINCT v.partNo) FROM VoterEntity v " +
		       "WHERE v.accountId = :accountId AND v.electionId = :electionId " +
		       "AND v.partNo = :partNumber")
		long countVotersByPart(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId,
		    @Param("partNumber") Integer partNumber
		);
		
		@Query("SELECT DISTINCT v.familyId FROM VoterEntity v " +
		       "WHERE v.accountId = :accountId AND v.electionId = :electionId " +
		       "AND v.familyId IN :familyIds")
		List<UUID> findExistingFamilyIds(
		    @Param("accountId") Long accountId,
		    @Param("electionId") Long electionId,
		    @Param("familyIds") List<UUID> familyIds
		);

		/**
		 * Get family options for dropdown in family captain assignment
		 * Returns family ID, family number, and family head name
		 */
		@Query("""
		    SELECT DISTINCT 
		        v.familyId,
		        v.familySequenceNumber,
		        v.voterFnameEn
		    FROM VoterEntity v 
		    WHERE v.accountId = :accountId 
		    AND v.electionId = :electionId 
		    AND v.familyId IS NOT NULL
		    AND (v.isFamilyHead = true OR v.familySequenceNumber IS NOT NULL)
		    AND (:searchTerm IS NULL OR 
		         LOWER(v.voterFnameEn) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
		         CAST(v.familySequenceNumber AS string) LIKE CONCAT('%', :searchTerm, '%'))
		    ORDER BY v.familySequenceNumber
		    """)
		List<Object[]> findFamilyOptionsForDropdown(@Param("accountId") Long accountId, 
		                                          @Param("electionId") Long electionId, 
		                                          @Param("searchTerm") String searchTerm);

		/**
		 * Get booth-wise party rankings for booth strength classification
		 * Uses window function to rank parties within each booth by voter count
		 */
		@Query(value = """
		    SELECT 
		        v.part_no as boothNumber,
		        v.party_id as partyId,
		        COUNT(*) as voterCount,
		        RANK() OVER (PARTITION BY v.part_no ORDER BY COUNT(*) DESC) as partyRank
		    FROM _voters v
		    WHERE v.election_id = :electionId 
		    AND v.account_id = :accountId
		    AND v.party_id IS NOT NULL
		    GROUP BY v.part_no, v.party_id
		    ORDER BY v.part_no, partyRank
		    """, nativeQuery = true)
		List<Object[]> getBoothPartyRankings(@Param("accountId") Long accountId,
		                                      @Param("electionId") Long electionId);

		/**
		 * Get all distinct booth numbers for an election
		 */
		@Query("SELECT DISTINCT v.partNo FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId ORDER BY v.partNo")
		List<Integer> findDistinctBoothNumbersByAccountIdAndElectionId(@Param("accountId") Long accountId,
		                                                                 @Param("electionId") Long electionId);

		/**
		 * Optimized export query with @ManyToOne relationships eagerly fetched
		 * Used for Excel export operations to avoid N+1 queries
		 * 
		 * PERFORMANCE NOTES:
		 * - Only fetches @ManyToOne relationships (no collections to avoid Cartesian product)
		 * - Collections use @BatchSize for efficient batch loading
		 * - No ORDER BY to avoid Hibernate in-memory pagination
		 * - Uses query hints for read-only optimization
		 */
		@Query("""
			SELECT v FROM VoterEntity v 
			LEFT JOIN FETCH v.religion 
			LEFT JOIN FETCH v.caste 
			LEFT JOIN FETCH v.subCaste 
			LEFT JOIN FETCH v.party 
			LEFT JOIN FETCH v.availability1 
			WHERE v.accountId = :accountId 
			AND v.electionId = :electionId
			""")
		@QueryHints({
			@QueryHint(name = "org.hibernate.fetchSize", value = "1000"),
			@QueryHint(name = "org.hibernate.readOnly", value = "true")
		})
		Page<VoterEntity> findAllForExportWithRelationships(
			@Param("accountId") Long accountId,
			@Param("electionId") Long electionId,
			Pageable pageable
		);

		/**
		 * Count voters for export (without fetching relationships)
		 */
		@Query("SELECT COUNT(v) FROM VoterEntity v WHERE v.accountId = :accountId AND v.electionId = :electionId")
		long countForExport(@Param("accountId") Long accountId, @Param("electionId") Long electionId);

		/**
		 * Get election-level voter statistics
		 */
		@Query("""
			SELECT 
				COUNT(v) as totalVoters,
				SUM(CASE WHEN v.hasVoted = true THEN 1 ELSE 0 END) as votedCount,
				SUM(CASE WHEN v.hasVoted = false OR v.hasVoted IS NULL THEN 1 ELSE 0 END) as notVotedCount
			FROM VoterEntity v 
			WHERE v.accountId = :accountId 
			AND v.electionId = :electionId
			""")
		Object[] getElectionVoterStats(@Param("accountId") Long accountId, @Param("electionId") Long electionId);

		/**
		 * Get default party statistics for winning probability calculation
		 */
		@Query("""
			SELECT 
				COUNT(v) as totalSupporters,
				SUM(CASE WHEN v.hasVoted = true THEN 1 ELSE 0 END) as votedSupporters
		FROM VoterEntity v 
		WHERE v.accountId = :accountId 
		AND v.electionId = :electionId 
		AND v.party.id = :partyId
		""")
	Object[] getPartyStats(@Param("accountId") Long accountId, 
	                       @Param("electionId") Long electionId, 
	                       @Param("partyId") Long partyId);

	/**
	 * Get unique dynamic field names for an election (for CSV export)
	 */
	@Query(value = """
		SELECT DISTINCT vdf.field_name 
		FROM voter_dynamic_fields vdf
		INNER JOIN _voters v ON v.id = vdf.voter_id
		WHERE v.account_id = :accountId 
		AND v.election_id = :electionId
		ORDER BY vdf.field_name
		""", nativeQuery = true)
	List<String> findUniqueDynamicFieldNames(@Param("accountId") Long accountId, 
	                                           @Param("electionId") Long electionId);

	/**
	 * Optimized gender statistics query with all filters - returns [maleCount, femaleCount, otherCount, totalCount]
	 * This replaces in-memory stats calculation with a single optimized database query
	 */
	@Query(value = """
SELECT 
COUNT(CASE WHEN LOWER(v.gender) IN ('male', 'm') THEN 1 END) as male_count,
COUNT(CASE WHEN LOWER(v.gender) IN ('female', 'f') THEN 1 END) as female_count,
COUNT(CASE WHEN v.gender IS NULL OR LOWER(v.gender) NOT IN ('male', 'm', 'female', 'f') THEN 1 END) as other_count,
COUNT(*) as total_count
FROM _voters v
LEFT JOIN _availability a ON v.availability_id = a.id
LEFT JOIN _party p ON v.party_id = p.id
LEFT JOIN _religion r ON v.religion_id = r.id
LEFT JOIN _caste_category cc ON v.caste_category_id = cc.id
LEFT JOIN _caste c ON v.caste_id = c.id
LEFT JOIN _sub_caste sc ON v.sub_caste_id = sc.id
WHERE v.account_id = :accountId 
AND v.election_id = :electionId
AND (:voterId IS NULL OR v.voter_id = :voterId)
AND (:epicNumber IS NULL OR v.epic_number = :epicNumber)
AND (:boothNumbers IS NULL OR v.part_no IN (:boothNumbers))
AND (:familyId IS NULL OR v.family_id = CAST(:familyId AS uuid))
AND (:friendId IS NULL OR v.friend_id = CAST(:friendId AS uuid))
AND (:voterFnameEn IS NULL OR LOWER(TRIM(v.voter_fname_en)) IN (:voterFnameEn))
AND (:voterLnameEn IS NULL OR LOWER(TRIM(v.voter_lname_en)) IN (:voterLnameEn))
AND (:voterFnameL1 IS NULL OR LOWER(TRIM(v.voter_fname_l1)) IN (:voterFnameL1))
AND (:voterFnameL2 IS NULL OR LOWER(TRIM(v.voter_fname_l2)) IN (:voterFnameL2))
AND (:voterLnameL1 IS NULL OR LOWER(TRIM(v.voter_lname_l1)) IN (:voterLnameL1))
AND (:voterLnameL2 IS NULL OR LOWER(TRIM(v.voter_lname_l2)) IN (:voterLnameL2))
AND (:relationFirstNameEn IS NULL OR LOWER(TRIM(v.rln_fname_en)) IN (:relationFirstNameEn))
AND (:relationLastNameEn IS NULL OR LOWER(TRIM(v.rln_lname_en)) IN (:relationLastNameEn))
AND (:rlnFnameL1 IS NULL OR LOWER(TRIM(v.rln_fname_l1)) IN (:rlnFnameL1))
AND (:rlnFnameL2 IS NULL OR LOWER(TRIM(v.rln_fname_l2)) IN (:rlnFnameL2))
AND (:rlnLnameL1 IS NULL OR LOWER(TRIM(v.rln_lname_l1)) IN (:rlnLnameL1))
AND (:rlnLnameL2 IS NULL OR LOWER(TRIM(v.rln_lname_l2)) IN (:rlnLnameL2))
AND (:voterHistoryName IS NULL OR EXISTS (
SELECT 1 FROM voter_voter_history vvh 
JOIN _voter_history vh ON vvh.voter_history_id = vh.id 
WHERE vvh.voter_id = v.id AND LOWER(TRIM(vh.voter_history_name)) IN (:voterHistoryName)
))
AND (:partyName IS NULL OR LOWER(TRIM(p.party_name)) IN (:partyName))
AND (:religionName IS NULL OR LOWER(TRIM(r.religion_name)) IN (:religionName))
AND (:age IS NULL OR v.age = :age)
AND (
((:includeUnknownAge IS NULL OR :includeUnknownAge = TRUE) AND 
(v.age IS NULL OR ((:minAge IS NULL OR v.age >= :minAge) AND (:maxAge IS NULL OR v.age <= :maxAge))))
OR
(:includeUnknownAge = FALSE AND v.age IS NOT NULL AND 
(:minAge IS NULL OR v.age >= :minAge) AND (:maxAge IS NULL OR v.age <= :maxAge))
)
AND (:genders IS NULL OR LOWER(v.gender) IN (:genders))
AND ((COALESCE(:filterToday, false) = false AND COALESCE(:filterTomorrow, false) = false AND :birthdayMonth IS NULL AND :birthdayDay IS NULL) OR
(v.dob IS NOT NULL AND (
(COALESCE(:filterToday, false) = true AND EXTRACT(MONTH FROM v.dob) = :todayMonth AND EXTRACT(DAY FROM v.dob) = :todayDay) OR
(COALESCE(:filterTomorrow, false) = true AND EXTRACT(MONTH FROM v.dob) = :tomorrowMonth AND EXTRACT(DAY FROM v.dob) = :tomorrowDay) OR
(:birthdayMonth IS NOT NULL AND :birthdayDay IS NOT NULL AND EXTRACT(MONTH FROM v.dob) = :birthdayMonth AND EXTRACT(DAY FROM v.dob) = :birthdayDay)
)))
AND (:starNumber IS NULL OR v.star_number = :starNumber)
AND (:description IS NULL OR LOWER(TRIM(a.description)) IN (:description))
AND (:categoryName IS NULL OR LOWER(TRIM(a.category_name)) IN (:categoryName))
AND (:casteCategoryName IS NULL OR LOWER(TRIM(cc.caste_category_name)) IN (:casteCategoryName))
AND (:casteName IS NULL OR LOWER(TRIM(c.caste_name)) IN (:casteName))
AND (:subCasteName IS NULL OR LOWER(TRIM(sc.sub_caste_name)) IN (:subCasteName))
AND (:overseas IS NULL OR 
(:overseas = true AND v.section_no = 999) OR 
(:overseas = false AND (v.section_no IS NULL OR v.section_no != 999)))
AND (:serialNo IS NULL OR v.serial_no = :serialNo)
AND (:fatherless IS NULL OR (:fatherless = true AND v.rln_type IS NOT NULL AND LOWER(v.rln_type) = 'mother'))
AND (:guardian IS NULL OR (:guardian = true AND v.rln_type IS NOT NULL AND LOWER(v.rln_type) = 'other'))
AND (:hasMobileNo IS NULL OR (:hasMobileNo = true AND v.mobile_no IS NOT NULL AND TRIM(v.mobile_no) != ''))
AND (:mobileNo IS NULL OR v.mobile_no = :mobileNo)
AND (:singleVoterFamily IS NULL OR (:singleVoterFamily = true AND v.family_id IS NOT NULL AND v.family_count = 1))
AND (:pollStatus IS NULL OR 
(:pollStatus = 'voted' AND v.has_voted = true) OR 
(:pollStatus = 'notVoted' AND (v.has_voted = false OR v.has_voted IS NULL)) OR 
(:pollStatus = 'all'))
""", nativeQuery = true)
Object[] getGenderStatsOptimized(
@Param("accountId") Long accountId,
@Param("electionId") Long electionId,
@Param("voterId") String voterId,
@Param("epicNumber") String epicNumber,
@Param("boothNumbers") List<Integer> boothNumbers,
@Param("familyId") String familyId,
@Param("friendId") String friendId,
@Param("voterFnameEn") List<String> voterFnameEn,
@Param("voterLnameEn") List<String> voterLnameEn,
@Param("voterFnameL1") List<String> voterFnameL1,
@Param("voterFnameL2") List<String> voterFnameL2,
@Param("voterLnameL1") List<String> voterLnameL1,
@Param("voterLnameL2") List<String> voterLnameL2,
@Param("relationFirstNameEn") List<String> relationFirstNameEn,
@Param("relationLastNameEn") List<String> relationLastNameEn,
@Param("rlnFnameL1") List<String> rlnFnameL1,
@Param("rlnFnameL2") List<String> rlnFnameL2,
@Param("rlnLnameL1") List<String> rlnLnameL1,
@Param("rlnLnameL2") List<String> rlnLnameL2,
@Param("partyName") List<String> partyName,
@Param("voterHistoryName") List<String> voterHistoryName,
@Param("religionName") List<String> religionName,
@Param("age") Integer age,
@Param("minAge") Integer minAge,
@Param("maxAge") Integer maxAge,
@Param("includeUnknownAge") Boolean includeUnknownAge,
@Param("genders") List<String> genders,
@Param("filterToday") Boolean filterToday,
@Param("filterTomorrow") Boolean filterTomorrow,
@Param("todayMonth") Integer todayMonth,
@Param("todayDay") Integer todayDay,
@Param("tomorrowMonth") Integer tomorrowMonth,
@Param("tomorrowDay") Integer tomorrowDay,
@Param("birthdayMonth") Integer birthdayMonth,
@Param("birthdayDay") Integer birthdayDay,
@Param("starNumber") Boolean starNumber,
@Param("description") List<String> description,
@Param("categoryName") List<String> categoryName,
@Param("casteCategoryName") List<String> casteCategoryName,
@Param("casteName") List<String> casteName,
@Param("subCasteName") List<String> subCasteName,
@Param("serialNo") Long serialNo,
@Param("overseas") Boolean overseas,
@Param("fatherless") Boolean fatherless,
@Param("guardian") Boolean guardian,
@Param("hasMobileNo") Boolean hasMobileNo,
@Param("mobileNo") String mobileNo,
@Param("singleVoterFamily") Boolean singleVoterFamily,
@Param("pollStatus") String pollStatus
);
}
