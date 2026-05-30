package com.thedal.thedal_app.volunteer;

import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.volunteer.dto.VolunteerDetailsDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerActivityResponseDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerLocationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MongoVolunteerService {
    @Autowired
    private MongoVolunteerRepository mongoVolunteerRepository;
    @Autowired
    private MongoVolunteerActivityRepository mongoVolunteerActivityRepository;
    @Autowired
    private MongoVolunteerDailyActivityRepository mongoVolunteerDailyActivityRepository;
    @Autowired
    private MongoVolunteerActivityLogRepository mongoVolunteerActivityLogRepository;

    // REMOVED: VolunteerServiceImpl dependency to break circular reference

    public ThedalResponse<VolunteerDetailsDTO> getVolunteerByUserId(Long userId, Long electionId) {
        List<MongoVolunteer> volunteers = mongoVolunteerRepository.findAll();
        MongoVolunteer found = volunteers.stream()
            .filter(v -> v.getUserEntity() != null && v.getUserEntity().getId().equals(userId)
                && v.getElectionEntity() != null && v.getElectionEntity().getId().equals(electionId))
            .findFirst()
            .orElse(null);
        if (found == null) {
            return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_FOUND, null);
        }
        VolunteerDetailsDTO dto = mapMongoVolunteerToDTO(found);
        return new ThedalResponse<>(ThedalSuccess.VOLUNTEER_FOUND, dto);
    }

    public ThedalResponse<Page<VolunteerLocationDto>> getAllVolunteersWithLocationData(int page, int size, Long userId, Long electionId) {
        Pageable pageable = PageRequest.of(page, size);
        List<MongoVolunteer> volunteers = mongoVolunteerRepository.findAll(pageable).getContent();
        List<VolunteerLocationDto> dtos = volunteers.stream()
            .filter(v -> (electionId == null || (v.getElectionEntity() != null && v.getElectionEntity().getId().equals(electionId)))
                && (userId == null || (v.getUserEntity() != null && v.getUserEntity().getId().equals(userId))))
            .map(v -> {
                VolunteerLocationDto dto = new VolunteerLocationDto();
                dto.setVolunteerId(v.getId() != null ? Long.valueOf(v.getId()) : null);
                dto.setUserId(v.getUserEntity() != null ? v.getUserEntity().getId() : null);
                dto.setFirstName(v.getUserEntity() != null ? v.getUserEntity().getFirstName() : null);
                dto.setLastName(v.getLastName());
                dto.setMobileNumber(v.getMobileNumber());
                dto.setAssignedBooth(v.getAssignedBooth());
                return dto;
            })
            .collect(Collectors.toList());
        Page<VolunteerLocationDto> resultPage = new PageImpl<>(dtos, pageable, dtos.size());
        return new ThedalResponse<>(ThedalSuccess.SUCCESS, resultPage);
    }

    public ThedalResponse<Page<VolunteerActivityResponseDTO>> getVolunteerActivity(Long userId, Long electionId, LocalDate startDate, LocalDate endDate, int page, int size) {
        Page<VolunteerActivityResponseDTO> emptyPage = Page.empty();
        return new ThedalResponse<>(ThedalSuccess.SUCCESS, emptyPage);
    }

    public ThedalResponse<List<VolunteerDetailsDTO>> getVolunteerByAssignedBoothsAndMobileNumber(Long electionId, List<Long> boothNumbers, String mobileNumber, Long userId) {
        List<MongoVolunteer> volunteers = mongoVolunteerRepository.findAll();
        List<VolunteerDetailsDTO> dtos = volunteers.stream()
            .filter(v -> {
                boolean matches = true;
                if (boothNumbers != null && !boothNumbers.isEmpty()) {
                    matches = v.getAssignedBooth() != null && v.getAssignedBooth().stream().anyMatch(boothNumbers::contains);
                }
                if (matches && mobileNumber != null && !mobileNumber.isEmpty()) {
                    matches = v.getMobileNumber() != null && v.getMobileNumber().equals(mobileNumber);
                }
                if (matches && electionId != null) {
                    matches = v.getElectionEntity() != null && v.getElectionEntity().getId().equals(electionId);
                }
                if (matches && userId != null) {
                    matches = v.getUserEntity() != null && v.getUserEntity().getId().equals(userId);
                }
                return matches;
            })
            .map(this::mapMongoVolunteerToDTO)
            .collect(Collectors.toList());
        return new ThedalResponse<>(ThedalSuccess.SUCCESS, dtos);
    }

    public ThedalResponse<Boolean> isCheckedIn(Long userId, Long electionId) {
        // TODO: Implement actual check-in status logic for MongoDB
        // For now, always return false (not checked in)
        return new ThedalResponse<>(ThedalSuccess.SUCCESS, false);
    }

    /**
     * Save volunteer daily activity to MongoDB
     */
    public void saveVolunteerDailyActivity(MongoVolunteerDailyActivity activity) {
        try {
            mongoVolunteerDailyActivityRepository.save(activity);
            log.info("Successfully saved volunteer daily activity to MongoDB: {}", activity.getId());
        } catch (Exception e) {
            log.error("Failed to save volunteer daily activity to MongoDB: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Save volunteer activity log to MongoDB
     */
    public void saveVolunteerActivityLog(MongoVolunteerActivityLog activityLog) {
        try {
            mongoVolunteerActivityLogRepository.save(activityLog);
            log.info("Successfully saved volunteer activity log to MongoDB: {}", activityLog.getId());
        } catch (Exception e) {
            log.error("Failed to save volunteer activity log to MongoDB: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update volunteer daily activity in MongoDB
     */
    public void updateVolunteerDailyActivity(MongoVolunteerDailyActivity activity) {
        try {
            mongoVolunteerDailyActivityRepository.save(activity);
            log.info("Successfully updated volunteer daily activity in MongoDB: {}", activity.getId());
        } catch (Exception e) {
            log.error("Failed to update volunteer daily activity in MongoDB: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Find volunteer daily activity by volunteer ID and date
     */
    public MongoVolunteerDailyActivity findVolunteerDailyActivity(Long volunteerId, LocalDate date) {
        try {
            // Implement the query logic based on your repository methods
            List<MongoVolunteerDailyActivity> activities = mongoVolunteerDailyActivityRepository.findAll();
            return activities.stream()
                .filter(activity -> activity.getVolunteerId().equals(volunteerId) 
                    && activity.getCheckInTime() != null 
                    && activity.getCheckInTime().toLocalDate().equals(date))
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            log.error("Failed to find volunteer daily activity in MongoDB: {}", e.getMessage(), e);
            return null;
        }
    }

    private VolunteerDetailsDTO mapMongoVolunteerToDTO(MongoVolunteer v) {
        VolunteerDetailsDTO dto = new VolunteerDetailsDTO();
        dto.setVolunteerId(v.getId() != null ? Long.valueOf(v.getId()) : null);
        dto.setFirstName(v.getUserEntity() != null ? v.getUserEntity().getFirstName() : null);
        dto.setLastName(v.getLastName());
        dto.setEmail(v.getEmail());
        dto.setMobileNumber(v.getMobileNumber());
        if (v.getVolunteerAddress() != null) {
            var addressDTO = new com.thedal.thedal_app.volunteer.dto.AddressDTO();
            addressDTO.setStreet(v.getVolunteerAddress().getStreet());
            addressDTO.setCity(v.getVolunteerAddress().getCity());
            addressDTO.setState(v.getVolunteerAddress().getState());
            addressDTO.setPostalCode(v.getVolunteerAddress().getPostalCode());
            addressDTO.setCountry(v.getVolunteerAddress().getCountry());
            dto.setAddress(addressDTO);
        }
        dto.setAssignedBooths(v.getAssignedBooth());
        dto.setStatus(v.getStatus());
        dto.setPhotoUrl(v.getPhotoUrl());
        dto.setRemarks(v.getRemarks());
        dto.setAccountId(v.getAccountId());
        dto.setGender(v.getGender());
        dto.setWhatsAppNumber(v.getWhatsAppNumber());
        dto.setUserId(v.getUserEntity() != null ? v.getUserEntity().getId() : null);
        dto.setRoleName(v.getUserEntity() != null && v.getUserEntity().getRole() != null ? v.getUserEntity().getRole().getRoleName() : null);
        return dto;
    }
}