package com.thedal.thedal_app.voter.dto;

import java.util.List;

import org.springframework.data.domain.Slice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoterResponseMongoDTO {
    private List<VoterResponseDTO1> voters;
    private GenderStatsDTO genderStats;
    private List<BoothGenderStatsDTO> boothGenderStats;
    private VerificationStatsDTO aadhaarStats;
    private VerificationStatsDTO membershipStats;
    private AddressedVoterStatsDTO addressedVoterStats;
    private List<BoothVerificationStatsDTO> boothAadhaarStats;
    private List<BoothVerificationStatsDTO> boothMembershipStats;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;
    
    public VoterResponseMongoDTO(List<VoterResponseDTO1> voters, GenderStatsDTO genderStats) {
        this.voters = voters;
        this.genderStats = genderStats;
        this.currentPage = 0;
        this.totalPages = 1;
        this.totalElements = voters != null ? voters.size() : 0;
        this.hasNext = false;
    }
    
    public VoterResponseMongoDTO(Slice<VoterResponseDTO1> voterSlice, GenderStatsDTO genderStats) {
        this.voters = voterSlice.getContent();
        this.genderStats = genderStats;
        this.currentPage = voterSlice.getNumber();
        this.totalPages = voterSlice.hasNext() ? voterSlice.getNumber() + 2 : voterSlice.getNumber() + 1;
        this.totalElements = voterSlice.getNumberOfElements();
        this.hasNext = voterSlice.hasNext();
    }
    
    // Constructor with all stats (similar to PostgreSQL VoterResponseDTO)
    public VoterResponseMongoDTO(
            List<VoterResponseDTO1> voters, 
            GenderStatsDTO genderStats,
            List<BoothGenderStatsDTO> boothGenderStats,
            VerificationStatsDTO aadhaarStats,
            VerificationStatsDTO membershipStats,
            AddressedVoterStatsDTO addressedVoterStats,
            List<BoothVerificationStatsDTO> boothAadhaarStats,
            List<BoothVerificationStatsDTO> boothMembershipStats) {
        this.voters = voters;
        this.genderStats = genderStats;
        this.boothGenderStats = boothGenderStats;
        this.aadhaarStats = aadhaarStats;
        this.membershipStats = membershipStats;
        this.addressedVoterStats = addressedVoterStats;
        this.boothAadhaarStats = boothAadhaarStats;
        this.boothMembershipStats = boothMembershipStats;
        this.currentPage = 0;
        this.totalPages = 1;
        this.totalElements = voters != null ? voters.size() : 0;
        this.hasNext = false;
    }

    // Constructor with slice and all stats
    public VoterResponseMongoDTO(
            Slice<VoterResponseDTO1> voterSlice, 
            GenderStatsDTO genderStats,
            List<BoothGenderStatsDTO> boothGenderStats,
            VerificationStatsDTO aadhaarStats,
            VerificationStatsDTO membershipStats,
            AddressedVoterStatsDTO addressedVoterStats,
            List<BoothVerificationStatsDTO> boothAadhaarStats,
            List<BoothVerificationStatsDTO> boothMembershipStats) {
        this.voters = voterSlice.getContent();
        this.genderStats = genderStats;
        this.boothGenderStats = boothGenderStats;
        this.aadhaarStats = aadhaarStats;
        this.membershipStats = membershipStats;
        this.addressedVoterStats = addressedVoterStats;
        this.boothAadhaarStats = boothAadhaarStats;
        this.boothMembershipStats = boothMembershipStats;
        this.currentPage = voterSlice.getNumber();
        this.totalPages = voterSlice.hasNext() ? voterSlice.getNumber() + 2 : voterSlice.getNumber() + 1;
        this.totalElements = voterSlice.getNumberOfElements();
        this.hasNext = voterSlice.hasNext();
    }
    
    // Constructor with proper pagination info including total count
    public VoterResponseMongoDTO(
            List<VoterResponseDTO1> voters, 
            GenderStatsDTO genderStats,
            List<BoothGenderStatsDTO> boothGenderStats,
            VerificationStatsDTO aadhaarStats,
            VerificationStatsDTO membershipStats,
            AddressedVoterStatsDTO addressedVoterStats,
            List<BoothVerificationStatsDTO> boothAadhaarStats,
            List<BoothVerificationStatsDTO> boothMembershipStats,
            long totalCount,
            int pageSize,
            int currentPage) {
        this.voters = voters;
        this.genderStats = genderStats;
        this.boothGenderStats = boothGenderStats;
        this.aadhaarStats = aadhaarStats;
        this.membershipStats = membershipStats;
        this.addressedVoterStats = addressedVoterStats;
        this.boothAadhaarStats = boothAadhaarStats;
        this.boothMembershipStats = boothMembershipStats;
        this.currentPage = currentPage;
        this.totalPages = (int) Math.ceil((double) totalCount / pageSize);
        this.totalElements = totalCount;
        this.hasNext = (currentPage + 1) < this.totalPages;
    }
}