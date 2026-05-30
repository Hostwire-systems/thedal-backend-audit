package com.thedal.thedal_app.voter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.files.Files;
import com.thedal.thedal_app.files.FilesRepository;
import com.thedal.thedal_app.files.HandlerType;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.quartz.JobSchedulerService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.dto.BulkUploadMemberStatusDto;
import com.thedal.thedal_app.voter.dto.BulkUploadResponse;
import com.thedal.thedal_app.voter.dto.MemberDTO;
import com.thedal.thedal_app.election.ElectionFreezeInterceptor.CheckElectionNotFrozen;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MemberService {
	
	@Autowired
    private MemberRepository memberRepository; 
	@Autowired
    private ElectionRepository electionRepository;
	@Autowired
    private RequestDetailsService requestDetails; 
	@Autowired
	private AwsFileUpload awsFileUpload;
	@Value("${aws.s3.files.bucket}")
    private String s3bucket; 
	@Autowired
    private FilesRepository filesRepo; 
	@Autowired
    private BulkUploadRepo bulkUploadRepo;
	@Autowired
    private JobSchedulerService jobSchedulerService;
	@Autowired
    private MemberFileUploadService memberFileUploadService;
	@Autowired
    private BulkUploadMemberRepository bulkUploadMemberRepo;
	@Autowired
    private BulkUploadErrorRepository bulkUploadErrorRepo;
	@Autowired
    private MemberMongoRepository memberMongoRepository;

	private void validateElectionOwnership(Long electionId, Long accountId) {
        Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
        if (!electionOpt.isPresent()) {
            log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);  
        }
    }
	
	@Transactional
	@CheckElectionNotFrozen(electionIdParamIndex = 0)
	public ThedalResponse<MemberDTO> saveMember(Long electionId, MemberDTO memberDto) throws DataIntegrityViolationException {
	    Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
	    
	    // Use electionId from parameter instead of DTO
	    memberDto.setElectionId(electionId);
	    // Validate mobile (pass null as it's a new entry)

	    Optional<MemberEntity> existingMemberOpt = memberRepository.findByMembershipNoAndElectionIdAndAccountId(
	            memberDto.getMembershipNo(), memberDto.getElectionId(), accountId);

	    MemberEntity member;
	    if (existingMemberOpt.isPresent()) {
	        member = existingMemberOpt.get();
	        log.info("Updating existing member with Membership No: {}", memberDto.getMembershipNo());
	    } else {
	        member = new MemberEntity();
	        member.setAccountId(accountId);
	        log.info("Creating new member record for Membership No: {}", memberDto.getMembershipNo());
	    }

        if (memberDto.getMobileNumber() != null) {
            Optional<MemberEntity> existing = memberRepository.findByMobileNumberAndElectionIdAndAccountId(
                memberDto.getMobileNumber(), memberDto.getElectionId(), accountId);
        
            if (existing.isPresent()) {
                throw new ThedalException(ThedalError.MOBILE_NUMBER_ALREADY_EXISTS, HttpStatus.CONFLICT);
            }
        }

	    // Copy properties, excluding primary key and accountId
	    BeanUtils.copyProperties(memberDto, member, "id", "accountId");

	    // Save to PostgreSQL and MongoDB with dual-write pattern
	    try {
	        MemberEntity savedMember = memberRepository.save(member);
	        try {
	            MemberMongo memberMongo = new MemberMongo(savedMember);
	            memberMongoRepository.save(memberMongo);
	            log.info("Successfully saved member to MongoDB: id={}, membershipNo={}", savedMember.getId(), savedMember.getMembershipNo());
	        } catch (Exception mongoEx) {
	            log.error("Failed to save member to MongoDB: id={}, membershipNo={}", savedMember.getId(), savedMember.getMembershipNo(), mongoEx);
	            throw new RuntimeException("MongoDB save failed, triggering rollback", mongoEx);
	        }
	        
	        MemberDTO responseDto = new MemberDTO(savedMember);
	        return new ThedalResponse<>(ThedalSuccess.MEMBER_CREATED, responseDto);
	    } catch (Exception ex) {
	        log.error("Failed to create member: {}", memberDto.getMembershipNo(), ex);
	        throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	public ThedalResponse<List<MemberDTO>> getAllMembers(Long electionId, Long accountId, String epicNumber) {
        log.info("Fetching all members from MongoDB for account ID: {} and election ID: {}", accountId, electionId);
        
        List<MemberMongo> members;

        if (epicNumber != null && !epicNumber.isEmpty()) {
            members = memberMongoRepository.findByEpicNumberAndAccountIdAndElectionId(epicNumber, accountId, electionId);
        } else {
            members = memberMongoRepository.findByAccountIdAndElectionId(accountId, electionId);
        }
        
        List<MemberDTO> memberDTOs = members.stream()
                .map(member -> {
                    MemberDTO dto = new MemberDTO();
                    // Copy MongoDB fields to DTO
                    dto.setId(member.getMemberId()); // Use PostgreSQL ID for compatibility
                    dto.setAccountId(member.getAccountId());
                    dto.setElectionId(member.getElectionId());
                    dto.setMemberName(member.getMemberName());
                    dto.setRelationName(member.getRelationName());
                    dto.setRelationType(member.getRelationType());
                    dto.setGender(member.getGender());
                    dto.setDateOfBirth(member.getDateOfBirth());
                    dto.setAge(member.getAge());
                    dto.setOccupation(member.getOccupation());
                    dto.setEducation(member.getEducation());
                    dto.setFullAddress(member.getFullAddress());
                    dto.setMobileNumber(member.getMobileNumber());
                    dto.setMemberSinceYear(member.getMemberSinceYear());
                    dto.setMembershipNo(member.getMembershipNo());
                    dto.setStateNameEn(member.getStateNameEn());
                    dto.setStateNameL1(member.getStateNameL1());
                    dto.setStateNameL2(member.getStateNameL2());
                    dto.setDistrictCode(member.getDistrictCode());
                    dto.setDistrictNameEn(member.getDistrictNameEn());
                    dto.setDistrictNameL1(member.getDistrictNameL1());
                    dto.setDistrictNameL2(member.getDistrictNameL2());
                    dto.setPcNo(member.getPcNo());
                    dto.setPcNameEn(member.getPcNameEn());
                    dto.setPcNameL1(member.getPcNameL1());
                    dto.setPcNameL2(member.getPcNameL2());
                    dto.setAcNo(member.getAcNo());
                    dto.setAcNameEn(member.getAcNameEn());
                    dto.setAcNameL1(member.getAcNameL1());
                    dto.setAcNameL2(member.getAcNameL2());
                    dto.setUrbanNo(member.getUrbanNo());
                    dto.setUrbanNameEn(member.getUrbanNameEn());
                    dto.setUrbanNameL1(member.getUrbanNameL1());
                    dto.setUrbanWardNo(member.getUrbanWardNo());
                    dto.setRurDistrictUnionNo(member.getRurDistrictUnionNo());
                    dto.setRurDistrictUnionNameEn(member.getRurDistrictUnionNameEn());
                    dto.setRurDistrictUnionNameL1(member.getRurDistrictUnionNameL1());
                    dto.setRurDistrictUnionNameL2(member.getRurDistrictUnionNameL2());
                    dto.setRurDistrictUnionWardNo(member.getRurDistrictUnionWardNo());
                    dto.setPanUnionNo(member.getPanUnionNo());
                    dto.setPanUnionNameEn(member.getPanUnionNameEn());
                    dto.setPanUnionNameL1(member.getPanUnionNameL1());
                    dto.setPanUnionNameL2(member.getPanUnionNameL2());
                    dto.setPanUnionWardNo(member.getPanUnionWardNo());
                    dto.setVillPanNo(member.getVillPanNo());
                    dto.setVillPanNameEn(member.getVillPanNameEn());
                    dto.setVillPanNameL1(member.getVillPanNameL1());
                    dto.setVillPanWardNo(member.getVillPanWardNo());
                    dto.setEpicNumber(member.getEpicNumber());
                    dto.setStateCode(member.getStateCode());
                    return dto;
                })
                .collect(Collectors.toList());

        log.info("Successfully fetched {} members from MongoDB for electionId: {}", memberDTOs.size(), electionId);
        return new ThedalResponse<>(ThedalSuccess.SUCCESS, memberDTOs);
    }

    @Transactional
    public ThedalResponse<List<MemberDTO>> getAllMembersFromMongo(Long electionId, Long accountId, String searchTerm) {
        log.info("Fetching all members from PostgreSQL for account ID: {} and election ID: {}", accountId, electionId);
        
        // Read from PostgreSQL instead of MongoDB
        List<MemberEntity> members = memberRepository.findByElectionIdAndAccountId(electionId, accountId);
        
        // Apply search filter if provided (simple name filter as the PostgreSQL repo doesn't have a search method)
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String searchLower = searchTerm.trim().toLowerCase();
            members = members.stream()
                    .filter(member -> member.getMemberName() != null && 
                                    member.getMemberName().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }
        
        if (members.isEmpty()) {
            log.warn("No members found in PostgreSQL for account ID: {} and election ID: {}", accountId, electionId);
            throw new ThedalException(ThedalError.MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        List<MemberDTO> memberDTOs = members.stream()
                .map(member -> {
                    MemberDTO dto = new MemberDTO();
                    // Copy PostgreSQL entity fields to DTO
                    dto.setId(member.getId()); 
                    dto.setAccountId(member.getAccountId());
                    dto.setElectionId(member.getElectionId());
                    dto.setMemberName(member.getMemberName());
                    dto.setRelationName(member.getRelationName());
                    dto.setRelationType(member.getRelationType());
                    dto.setGender(member.getGender());
                    dto.setDateOfBirth(member.getDateOfBirth());
                    dto.setAge(member.getAge());
                    dto.setOccupation(member.getOccupation());
                    dto.setEducation(member.getEducation());
                    dto.setFullAddress(member.getFullAddress());
                    dto.setMobileNumber(member.getMobileNumber());
                    dto.setMemberSinceYear(member.getMemberSinceYear());
                    dto.setMembershipNo(member.getMembershipNo());
                    dto.setStateNameEn(member.getStateNameEn());
                    dto.setDistrictNameEn(member.getDistrictNameEn());
                    dto.setPcNameEn(member.getPcNameEn());
                    dto.setAcNameEn(member.getAcNameEn());
                    dto.setEpicNumber(member.getEpicNumber());
                    dto.setStateCode(member.getStateCode());
                    // Add other fields as needed
                    return dto;
                })
                .collect(Collectors.toList());

        log.info("Successfully fetched {} members from PostgreSQL for electionId: {}", memberDTOs.size(), electionId);
        return new ThedalResponse<>(ThedalSuccess.SUCCESS, memberDTOs);
    }

    // private void validateUniqueMobile(MemberDTO dto, Long accountId, Long existingMemberId) {
    //     if (dto.getMobileNumber() != null) {
    //         Optional<MemberEntity> existing = memberRepository.findByMobileNumberAndElectionIdAndAccountId(
    //                 dto.getMobileNumber(), dto.getElectionId(), accountId);
    //         if (existing.isPresent() && !existing.get().getId().equals(existingMemberId)) {
    //             throw new ThedalException(ThedalError.MOBILE_NUMBER_ALREADY_EXISTS, HttpStatus.CONFLICT);
    //         }
    //     }
    // }
    
//	public ThedalResponse<MemberDTO> getMemberByMembershipNoAndElectionId(Long electionId, String membershipNo, Long accountId, String epicNumber) {
//		Optional<MemberEntity> member;
//
//	    if (epicNumber != null && !epicNumber.isEmpty()) {
//	        member = memberRepository.findByElectionIdAndMembershipNoAndAccountIdAndEpicNumber(electionId, membershipNo, accountId, epicNumber);
//	    } else {
//	        member = memberRepository.findByElectionIdAndMembershipNoAndAccountId(electionId, membershipNo, accountId);
//	    }
//
//	    MemberEntity memberEntity = member.orElseThrow(() -> new ThedalException(ThedalError.MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND));
//
//	    return new ThedalResponse<>(ThedalSuccess.SUCCESS, new MemberDTO(memberEntity));
//	}
	public ThedalResponse<MemberDTO> getMemberByMembershipNoOrEpicNumber(
	        Long electionId,
	        String membershipNo,
	        String epicNumber,
	        Long accountId) {

	    log.info("Fetching member from MongoDB for electionId: {}, membershipNo: {}, epicNumber: {}", electionId, membershipNo, epicNumber);
	    
	    List<MemberMongo> members = null;

	    if (membershipNo != null && !membershipNo.isEmpty() && epicNumber != null && !epicNumber.isEmpty()) {
	        members = memberMongoRepository.findByMembershipNoAndEpicNumberAndAccountIdAndElectionId(
	                membershipNo, epicNumber, accountId, electionId);
	    } else if (membershipNo != null && !membershipNo.isEmpty()) {
	        members = memberMongoRepository.findByMembershipNoAndAccountIdAndElectionId(
	                membershipNo, accountId, electionId);
	    } else if (epicNumber != null && !epicNumber.isEmpty()) {
	        members = memberMongoRepository.findByEpicNumberAndAccountIdAndElectionId(
	                epicNumber, accountId, electionId);
	    }

	    if (members == null || members.isEmpty()) {
	        throw new ThedalException(ThedalError.MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND);
	    }
	    
	    MemberMongo memberMongo = members.get(0); // Take first result
	    
	    // Convert to DTO
	    MemberDTO dto = new MemberDTO();
	    dto.setId(memberMongo.getMemberId()); // Use PostgreSQL ID for compatibility
	    dto.setAccountId(memberMongo.getAccountId());
	    dto.setElectionId(memberMongo.getElectionId());
	    dto.setMemberName(memberMongo.getMemberName());
	    dto.setRelationName(memberMongo.getRelationName());
	    dto.setRelationType(memberMongo.getRelationType());
	    dto.setGender(memberMongo.getGender());
	    dto.setDateOfBirth(memberMongo.getDateOfBirth());
	    dto.setAge(memberMongo.getAge());
	    dto.setOccupation(memberMongo.getOccupation());
	    dto.setEducation(memberMongo.getEducation());
	    dto.setFullAddress(memberMongo.getFullAddress());
	    dto.setMobileNumber(memberMongo.getMobileNumber());
	    dto.setMemberSinceYear(memberMongo.getMemberSinceYear());
	    dto.setMembershipNo(memberMongo.getMembershipNo());
	    dto.setStateNameEn(memberMongo.getStateNameEn());
	    dto.setDistrictNameEn(memberMongo.getDistrictNameEn());
	    dto.setPcNameEn(memberMongo.getPcNameEn());
	    dto.setAcNameEn(memberMongo.getAcNameEn());
	    dto.setEpicNumber(memberMongo.getEpicNumber());
	    dto.setStateCode(memberMongo.getStateCode());
	    // Add other fields as needed

	    return new ThedalResponse<>(ThedalSuccess.SUCCESS, dto);
	}



	public ThedalResponse<MemberDTO> updateMember(Long electionId, Long memberId, Long accountId, MemberDTO memberDTO) {
	    MemberEntity member = memberRepository.findByElectionIdAndIdAndAccountId(electionId, memberId, accountId)
	            .orElseThrow(() -> new ThedalException(ThedalError.MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND));
                if (memberDTO.getMobileNumber() != null) {
                    Optional<MemberEntity> existing = memberRepository.findByMobileNumberAndElectionIdAndAccountId(
                        memberDTO.getMobileNumber(), electionId, accountId);
                
                    if (existing.isPresent() && !existing.get().getId().equals(memberId)) {
                        throw new ThedalException(ThedalError.MOBILE_NUMBER_ALREADY_EXISTS, HttpStatus.CONFLICT);
                    }
                }
                
		if (memberDTO.getMemberName() != null) member.setMemberName(memberDTO.getMemberName());
		if (memberDTO.getRelationName() != null) member.setRelationName(memberDTO.getRelationName());
		if (memberDTO.getRelationType() != null) member.setRelationType(memberDTO.getRelationType());
		if (memberDTO.getGender() != null) member.setGender(memberDTO.getGender());
		if (memberDTO.getDateOfBirth() != null) member.setDateOfBirth(memberDTO.getDateOfBirth());
		if (memberDTO.getAge() != null) member.setAge(memberDTO.getAge());
		if (memberDTO.getOccupation() != null) member.setOccupation(memberDTO.getOccupation());
		if (memberDTO.getEducation() != null) member.setEducation(memberDTO.getEducation());
		if (memberDTO.getFullAddress() != null) member.setFullAddress(memberDTO.getFullAddress());
		if (memberDTO.getMobileNumber() != null) member.setMobileNumber(memberDTO.getMobileNumber());
		if (memberDTO.getMemberSinceYear() != null) member.setMemberSinceYear(memberDTO.getMemberSinceYear());
		if (memberDTO.getMembershipNo() != null) member.setMembershipNo(memberDTO.getMembershipNo());
		if (memberDTO.getStateNameEn() != null) member.setStateNameEn(memberDTO.getStateNameEn());
		if (memberDTO.getDistrictNameEn() != null) member.setDistrictNameEn(memberDTO.getDistrictNameEn());
		if (memberDTO.getPcNameEn() != null) member.setPcNameEn(memberDTO.getPcNameEn());
		if (memberDTO.getAcNameEn() != null) member.setAcNameEn(memberDTO.getAcNameEn());
	
		//  Add all the missing fields
		if (memberDTO.getStateCode() != null) member.setStateCode(memberDTO.getStateCode());
		if (memberDTO.getDistrictNameL1() != null) member.setDistrictNameL1(memberDTO.getDistrictNameL1());
		if (memberDTO.getDistrictNameL2() != null) member.setDistrictNameL2(memberDTO.getDistrictNameL2());
		if (memberDTO.getPcNo() != null) member.setPcNo(memberDTO.getPcNo());
		if (memberDTO.getPcNameL1() != null) member.setPcNameL1(memberDTO.getPcNameL1());
		if (memberDTO.getPcNameL2() != null) member.setPcNameL2(memberDTO.getPcNameL2());
		if (memberDTO.getAcNo() != null) member.setAcNo(memberDTO.getAcNo());
		if (memberDTO.getAcNameL1() != null) member.setAcNameL1(memberDTO.getAcNameL1());
		if (memberDTO.getAcNameL2() != null) member.setAcNameL2(memberDTO.getAcNameL2());
		if (memberDTO.getUrbanNo() != null) member.setUrbanNo(memberDTO.getUrbanNo());
		if (memberDTO.getUrbanNameEn() != null) member.setUrbanNameEn(memberDTO.getUrbanNameEn());
		if (memberDTO.getUrbanNameL1() != null) member.setUrbanNameL1(memberDTO.getUrbanNameL1());
		if (memberDTO.getUrbanWardNo() != null) member.setUrbanWardNo(memberDTO.getUrbanWardNo());
		if (memberDTO.getRurDistrictUnionNo() != null) member.setRurDistrictUnionNo(memberDTO.getRurDistrictUnionNo());
		if (memberDTO.getRurDistrictUnionNameEn() != null) member.setRurDistrictUnionNameEn(memberDTO.getRurDistrictUnionNameEn());
		if (memberDTO.getRurDistrictUnionNameL1() != null) member.setRurDistrictUnionNameL1(memberDTO.getRurDistrictUnionNameL1());
		if (memberDTO.getRurDistrictUnionNameL2() != null) member.setRurDistrictUnionNameL2(memberDTO.getRurDistrictUnionNameL2());
		if (memberDTO.getRurDistrictUnionWardNo() != null) member.setRurDistrictUnionWardNo(memberDTO.getRurDistrictUnionWardNo());
		if (memberDTO.getPanUnionNo() != null) member.setPanUnionNo(memberDTO.getPanUnionNo());
		if (memberDTO.getPanUnionNameEn() != null) member.setPanUnionNameEn(memberDTO.getPanUnionNameEn());
		if (memberDTO.getPanUnionNameL1() != null) member.setPanUnionNameL1(memberDTO.getPanUnionNameL1());
		if (memberDTO.getPanUnionNameL2() != null) member.setPanUnionNameL2(memberDTO.getPanUnionNameL2());
		if (memberDTO.getPanUnionWardNo() != null) member.setPanUnionWardNo(memberDTO.getPanUnionWardNo());
		if (memberDTO.getVillPanNo() != null) member.setVillPanNo(memberDTO.getVillPanNo());
		if (memberDTO.getVillPanNameEn() != null) member.setVillPanNameEn(memberDTO.getVillPanNameEn());
		if (memberDTO.getVillPanNameL1() != null) member.setVillPanNameL1(memberDTO.getVillPanNameL1());
		if (memberDTO.getVillPanWardNo() != null) member.setVillPanWardNo(memberDTO.getVillPanWardNo());
		if (memberDTO.getEpicNumber() != null) member.setEpicNumber(memberDTO.getEpicNumber());
		
		// Save to PostgreSQL and MongoDB with dual-write pattern
		try {
		    MemberEntity updatedMember = memberRepository.save(member);
		    try {
		        MemberMongo memberMongo = new MemberMongo(updatedMember);
		        memberMongoRepository.save(memberMongo);
		        log.info("Successfully updated member in MongoDB: id={}, membershipNo={}", updatedMember.getId(), updatedMember.getMembershipNo());
		    } catch (Exception mongoEx) {
		        log.error("Failed to update member in MongoDB: id={}, membershipNo={}", updatedMember.getId(), updatedMember.getMembershipNo(), mongoEx);
		        throw new RuntimeException("MongoDB update failed, triggering rollback", mongoEx);
		    }
		    
		    return new ThedalResponse<>(ThedalSuccess.SUCCESS, new MemberDTO(updatedMember));
		} catch (Exception ex) {
		    log.error("Failed to update member: id={}", memberId, ex);
		    throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Transactional
	public void deleteMembers(Long electionId, Long accountId, List<Long> memberIds) {
        log.info("Deleting members with IDs: {} for account ID: {} and election ID: {}", memberIds, accountId, electionId);
        
        try {
            if (memberIds == null || memberIds.isEmpty()) {
                log.info("Deleting all members for electionId: {}, accountId: {}", electionId, accountId);
                int deletedCount = memberRepository.deleteByElectionIdAndAccountId(electionId, accountId);
                if (deletedCount == 0) {
                    throw new ThedalException(ThedalError.MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND);
                }
                
                try {
                    memberMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);
                    log.info("Deleted all members from MongoDB for accountId: {}, electionId: {}", accountId, electionId);
                } catch (Exception mongoEx) {
                    log.error("Failed to delete all members from MongoDB for accountId: {}, electionId: {}", accountId, electionId, mongoEx);
                    throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
                }
            } else {
                log.info("Deleting selected members for electionId: {}, accountId: {}, memberIds: {}", electionId, accountId, memberIds);
                int deletedCount = memberRepository.deleteByElectionIdAndAccountIdAndIdIn(electionId, accountId, memberIds);
                if (deletedCount == 0) {
                    throw new ThedalException(ThedalError.MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND);
                }
                
                try {
                    memberMongoRepository.deleteByMemberIdIn(memberIds);
                    log.info("Deleted members from MongoDB: ids={}", memberIds);
                } catch (Exception mongoEx) {
                    log.error("Failed to delete members from MongoDB: ids={}", memberIds, mongoEx);
                    throw new RuntimeException("MongoDB deletion failed, triggering rollback", mongoEx);
                }
            }
            log.info("Members deleted successfully: ids={}", memberIds);
        } catch (Exception ex) {
            log.error("Failed to delete members: ids={}", memberIds, ex);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

	@Transactional
    public ThedalResponse<BulkUploadResponse> uploadMembersFromXlsxOrCsv(MultipartFile file, Long electionId) throws IOException {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        if (file == null || !isSupportedFormat(file.getOriginalFilename()) || file.isEmpty()) {
            throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        Set<String> mandatoryHeaders = Set.of("member_name", "gender", "mobile_number");

        String folder = "member_uploads";
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = folder + "/member_" + System.currentTimeMillis() + "_" + uniqueId + fileExtension;

        String fileUrl = null;
        Long bulkUploadId = null;

        try {
            Map<String, Integer> headerMapping = validateHeaders(file, fileExtension, mandatoryHeaders, electionId, accountId);

            log.info("Uploading file to S3: {}", uniqueFileName);
            fileUrl = awsFileUpload.uploadMultipartFile(file, uniqueFileName, s3bucket);

            BulkUploadMemberEntity bulkUploadEntity = new BulkUploadMemberEntity(accountId, electionId, LocalDateTime.now(), BulkUploadStatus.IN_PROGRESS);
            bulkUploadMemberRepo.save(bulkUploadEntity);
            bulkUploadId = bulkUploadEntity.getId();

            Files fileEntity = new Files(HandlerType.BULKUPLOAD_FILES, electionId, originalFileName, fileUrl);
            fileEntity.setBulkUploadMember(bulkUploadEntity);
            Files files = filesRepo.save(fileEntity);

            jobSchedulerService.scheduleMemberFileUploadJob(bulkUploadId, accountId, electionId, files.getId(), headerMapping, mandatoryHeaders);

            BulkUploadResponse bulkUploadResponse = new BulkUploadResponse(bulkUploadId);
            return new ThedalResponse<>(ThedalSuccess.BULK_MEMBERS_UPLOAD_IN_QUEUE, bulkUploadResponse);

        } catch (ThedalException te) {
            throw te;
        } catch (Exception e) {
            log.error("Error processing file '{}': {}", originalFileName, e.getMessage(), e);
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Integer> validateHeaders(MultipartFile file, String fileExtension, Set<String> mandatoryHeaders, Long electionId, Long accountId) throws IOException {
        List<String> missingMandatoryHeaders = new ArrayList<>();
        Map<String, Integer> headerMapping;

        if (fileExtension.equalsIgnoreCase(".xlsx")) {
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null || sheet.getRow(0) == null) {
                    throw new ThedalException(ThedalError.INVALID_FILE_DATA, HttpStatus.BAD_REQUEST);
                }
                headerMapping = buildHeaderMapping(sheet.getRow(0));
            }
        } else { // CSV
            try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String headerLine = br.readLine();
                if (headerLine == null || headerLine.trim().isEmpty()) {
                    throw new ThedalException(ThedalError.INVALID_FILE_DATA, HttpStatus.BAD_REQUEST);
                }
                String[] headers = headerLine.split(",");
                headerMapping = buildCsvHeaderMapping(headers);
            }
        }

        missingMandatoryHeaders = mandatoryHeaders.stream()
                .filter(header -> !headerMapping.containsKey(header))
                .collect(Collectors.toList());

        if (!missingMandatoryHeaders.isEmpty()) {
            BulkUploadErrorEntity error = new BulkUploadErrorEntity();
            error.setElectionId(electionId);
            error.setAccountId(accountId);
            try {
                error.setHeaderErrors(new ObjectMapper().writeValueAsString(missingMandatoryHeaders));
            } catch (JsonProcessingException e) {
                error.setHeaderErrors(String.join(",", missingMandatoryHeaders));
            }
            bulkUploadErrorRepo.save(error);
            throw new ThedalException(ThedalError.MANDATORY_FIELDS_MISSING, HttpStatus.BAD_REQUEST,
                    "Missing mandatory fields: " + String.join(",", missingMandatoryHeaders));
        }

        return headerMapping;
    }

    private Map<String, Integer> buildHeaderMapping(Row headerRow) {
        Map<String, Integer> headerMapping = new HashMap<>();
        for (Cell cell : headerRow) {
            if (cell.getCellType() == CellType.STRING) {
                String normalizedHeader = normalizeHeader(cell.getStringCellValue());
                headerMapping.put(normalizedHeader, cell.getColumnIndex());
            }
        }
        return headerMapping;
    }

    private Map<String, Integer> buildCsvHeaderMapping(String[] headers) {
        Map<String, Integer> headerMapping = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String normalizedHeader = normalizeHeader(headers[i]);
            headerMapping.put(normalizedHeader, i);
        }
        return headerMapping;
    }

    private String normalizeHeader(String header) {
        return header.trim().toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    private boolean isSupportedFormat(String originalFileName) {
        return originalFileName != null && (originalFileName.endsWith(".xlsx") || originalFileName.endsWith(".csv"));
    }

    @Transactional
    public ThedalResponse<Void> startUploadMembersFromXlsxOrCsv(Long accountId, Long bulkUploadId, Long electionId, Long fileId,
            Map<String, Integer> headerMapping, Set<String> mandatoryHeaders) {
        Files fileMetadata = filesRepo.findById(fileId)
                .orElseThrow(() -> new ThedalException(ThedalError.FILE_NOT_FOUND, HttpStatus.NOT_FOUND));

        String fileUrl = fileMetadata.getUrl();
        String fileName = fileMetadata.getFileName();

        try {
            if (fileName.endsWith(".xlsx")) {
                memberFileUploadService.processExcelFileAsync(bulkUploadId, accountId, electionId, fileUrl, headerMapping, mandatoryHeaders);
            } else if (fileName.endsWith(".csv")) {
                memberFileUploadService.processCsvFileAsync(bulkUploadId, accountId, electionId, fileUrl, headerMapping, mandatoryHeaders);
            }
            return new ThedalResponse<>(ThedalSuccess.BULK_MEMBERS_CREATED);
        } catch (Exception e) {
            log.error("Error processing file for fileId {}: {}", fileId, e.getMessage(), e);
            updateBulkUploadStatus(bulkUploadId, BulkUploadStatus.FAILED);
            throw new ThedalException(ThedalError.FILE_PROCESSING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void updateBulkUploadStatus(Long bulkUploadId, BulkUploadStatus status) {
        BulkUploadMemberEntity bulkUploadEntity = bulkUploadMemberRepo.findById(bulkUploadId)
                .orElseThrow(() -> new ThedalException(ThedalError.BULK_UPLOAD_NOT_FOUND, HttpStatus.NOT_FOUND));
        bulkUploadEntity.setStatus(status);
        if (status == BulkUploadStatus.COMPLETED || status == BulkUploadStatus.FAILED) {
            bulkUploadEntity.setEndTime(LocalDateTime.now());
        }
        bulkUploadMemberRepo.save(bulkUploadEntity);
    }

    public BulkUploadMemberStatusDto getBulkUploadMemberStatus(Long bulkUploadId, Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        validateElectionOwnership(electionId, accountId);

        BulkUploadMemberEntity bulkUploadEntity = bulkUploadMemberRepo.findByIdAndAccountIdAndElectionId(bulkUploadId, accountId, electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.BULK_UPLOAD_NOT_FOUND, HttpStatus.NOT_FOUND));

        return new BulkUploadMemberStatusDto(
                bulkUploadEntity.getId(),
                bulkUploadEntity.getStatus(),
                bulkUploadEntity.getStartTime(),
                bulkUploadEntity.getEndTime(),
                bulkUploadEntity.getTotalProcessedMembers(),
                bulkUploadEntity.getTotalFailedMembers(),
                bulkUploadEntity.getTotalRecords(),
                bulkUploadEntity.getTotalSuccessMembers()
        );
    }

    @Transactional
    public ThedalResponse<List<MemberDTO>> getAllMembersFromMongoDetailed(Long accountId, Long electionId) {
        log.info("Fetching all members from PostgreSQL for account ID: {} and election ID: {}", accountId, electionId);

        // Read from PostgreSQL instead of MongoDB
        List<MemberEntity> members = memberRepository.findByElectionIdAndAccountId(electionId, accountId);
        if (members.isEmpty()) {
            log.warn("No members found in PostgreSQL for account ID: {} and election ID: {}", accountId, electionId);
            throw new ThedalException(ThedalError.MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        List<MemberDTO> memberDetails = members.stream()
                .map(member -> {
                    MemberDTO dto = new MemberDTO();
                    // Copy all PostgreSQL entity fields to DTO
                    dto.setId(member.getId());
                    dto.setAccountId(member.getAccountId());
                    dto.setElectionId(member.getElectionId());
                    dto.setMemberName(member.getMemberName() != null ? member.getMemberName() : "");
                    dto.setRelationName(member.getRelationName() != null ? member.getRelationName() : "");
                    dto.setRelationType(member.getRelationType() != null ? member.getRelationType() : "");
                    dto.setGender(member.getGender() != null ? member.getGender() : "");
                    dto.setDateOfBirth(member.getDateOfBirth());
                    dto.setAge(member.getAge() != null ? member.getAge() : 0);
                    dto.setOccupation(member.getOccupation() != null ? member.getOccupation() : "");
                    dto.setEducation(member.getEducation() != null ? member.getEducation() : "");
                    dto.setFullAddress(member.getFullAddress() != null ? member.getFullAddress() : "");
                    dto.setMobileNumber(member.getMobileNumber() != null ? member.getMobileNumber() : "");
                    dto.setMemberSinceYear(member.getMemberSinceYear() != null ? member.getMemberSinceYear() : 0);
                    dto.setMembershipNo(member.getMembershipNo() != null ? member.getMembershipNo() : "");
                    dto.setStateNameEn(member.getStateNameEn() != null ? member.getStateNameEn() : "");
                    dto.setStateNameL1(member.getStateNameL1() != null ? member.getStateNameL1() : "");
                    dto.setStateNameL2(member.getStateNameL2() != null ? member.getStateNameL2() : "");
                    dto.setDistrictCode(member.getDistrictCode() != null ? member.getDistrictCode() : "");
                    dto.setDistrictNameEn(member.getDistrictNameEn() != null ? member.getDistrictNameEn() : "");
                    dto.setDistrictNameL1(member.getDistrictNameL1() != null ? member.getDistrictNameL1() : "");
                    dto.setDistrictNameL2(member.getDistrictNameL2() != null ? member.getDistrictNameL2() : "");
                    dto.setPcNo(member.getPcNo() != null ? member.getPcNo() : "");
                    dto.setPcNameEn(member.getPcNameEn() != null ? member.getPcNameEn() : "");
                    dto.setPcNameL1(member.getPcNameL1() != null ? member.getPcNameL1() : "");
                    dto.setPcNameL2(member.getPcNameL2() != null ? member.getPcNameL2() : "");
                    dto.setAcNo(member.getAcNo() != null ? member.getAcNo() : "");
                    dto.setAcNameEn(member.getAcNameEn() != null ? member.getAcNameEn() : "");
                    dto.setAcNameL1(member.getAcNameL1() != null ? member.getAcNameL1() : "");
                    dto.setAcNameL2(member.getAcNameL2() != null ? member.getAcNameL2() : "");
                    dto.setUrbanNo(member.getUrbanNo() != null ? member.getUrbanNo() : "");
                    dto.setUrbanNameEn(member.getUrbanNameEn() != null ? member.getUrbanNameEn() : "");
                    dto.setUrbanNameL1(member.getUrbanNameL1() != null ? member.getUrbanNameL1() : "");
                    dto.setUrbanWardNo(member.getUrbanWardNo() != null ? member.getUrbanWardNo() : "");
                    dto.setRurDistrictUnionNo(member.getRurDistrictUnionNo() != null ? member.getRurDistrictUnionNo() : "");
                    dto.setRurDistrictUnionNameEn(member.getRurDistrictUnionNameEn() != null ? member.getRurDistrictUnionNameEn() : "");
                    dto.setRurDistrictUnionNameL1(member.getRurDistrictUnionNameL1() != null ? member.getRurDistrictUnionNameL1() : "");
                    dto.setRurDistrictUnionNameL2(member.getRurDistrictUnionNameL2() != null ? member.getRurDistrictUnionNameL2() : "");
                    dto.setRurDistrictUnionWardNo(member.getRurDistrictUnionWardNo() != null ? member.getRurDistrictUnionWardNo() : "");
                    dto.setPanUnionNo(member.getPanUnionNo() != null ? member.getPanUnionNo() : "");
                    dto.setPanUnionNameEn(member.getPanUnionNameEn() != null ? member.getPanUnionNameEn() : "");
                    dto.setPanUnionNameL1(member.getPanUnionNameL1() != null ? member.getPanUnionNameL1() : "");
                    dto.setPanUnionNameL2(member.getPanUnionNameL2() != null ? member.getPanUnionNameL2() : "");
                    dto.setPanUnionWardNo(member.getPanUnionWardNo() != null ? member.getPanUnionWardNo() : "");
                    dto.setVillPanNo(member.getVillPanNo() != null ? member.getVillPanNo() : "");
                    dto.setVillPanNameEn(member.getVillPanNameEn() != null ? member.getVillPanNameEn() : "");
                    dto.setVillPanNameL1(member.getVillPanNameL1() != null ? member.getVillPanNameL1() : "");
                    dto.setVillPanWardNo(member.getVillPanWardNo() != null ? member.getVillPanWardNo() : "");
                    dto.setEpicNumber(member.getEpicNumber() != null ? member.getEpicNumber() : "");
                    dto.setStateCode(member.getStateCode() != null ? member.getStateCode() : "");
                    return dto;
                })
                .collect(Collectors.toList());

        log.info("Successfully fetched {} members from PostgreSQL for electionId: {}", memberDetails.size(), electionId);
        return new ThedalResponse<>(ThedalSuccess.SUCCESS, memberDetails);
    }

}
