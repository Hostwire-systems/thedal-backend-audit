package com.thedal.thedal_app.familycaptain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.familycaptain.dto.FamilyCaptainDetailsDTO;
import com.thedal.thedal_app.familycaptain.dto.FamilyCaptainDetailsUpdate;
import com.thedal.thedal_app.familycaptain.dto.FamilyCaptainUploadSummary;
import com.thedal.thedal_app.familycaptain.dto.FamilyDetailsDTO;
import com.thedal.thedal_app.familycaptain.dto.FamilyUpdateRequest;
import com.thedal.thedal_app.familycaptain.dto.SaveFamilyCaptainDetailsDTO;
import com.thedal.thedal_app.response.ThedalResponse;

import jakarta.validation.Valid;

@Service
public interface FamilyCaptainService {

    // Core CRUD operations
    ThedalResponse<Void> saveFamilyCaptain(SaveFamilyCaptainDetailsDTO familyCaptainDto, Long electionId);

    ThedalResponse<FamilyCaptainDetailsDTO> getFamilyCaptainByUserId(Long userId, Long electionId);

    ThedalResponse<Void> updateFamilyCaptain(Long userId, Long electionId, FamilyCaptainDetailsUpdate familyCaptainUpdate);

    ThedalResponse<Void> deleteFamilyCaptain(Long userId, Long electionId);

    ThedalResponse<Void> deleteFamilyCaptains(Long electionId, List<Long> userIdList);

    // Family assignment operations
    ThedalResponse<Void> updateAssignedFamilies(Long electionId, Long userId, FamilyUpdateRequest familyUpdateRequest);

    // Search and filter operations
    ThedalResponse<Page<FamilyCaptainDetailsDTO>> getFamilyCaptainsByAssignedFamiliesAndMobileNumber(
        Long electionId, 
        List<UUID> assignedFamilies, 
        String mobileNumber, 
        String searchTerm, 
        int page, 
        int size, 
        String sortBy, 
        String direction
    );

    // Bulk operations
    ThedalResponse<FamilyCaptainUploadSummary> uploadFamilyCaptainsFromXlsxOrCsv(MultipartFile file, Long electionId);

    // Utility operations
    ThedalResponse<List<FamilyCaptainDetailsDTO>> getFamilyCaptainsByAssignedFamily(UUID familyId, Long electionId);
    /**
     * Get available families for family captain assignment
     * Returns families with format "Family No - Family Head Name"
     */
    ThedalResponse<Page<FamilyDetailsDTO>> getFamilyOptions(Long electionId, String searchTerm, int page, int size);
}
