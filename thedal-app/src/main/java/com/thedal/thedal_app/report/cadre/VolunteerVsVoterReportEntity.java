package com.thedal.thedal_app.report.cadre;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "volunteer_vs_voter_report")
public class VolunteerVsVoterReportEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private Long electionId;
	
	//@Column(nullable = false)
	@Column(name = "volunteer_id")
	private Long volunteerId;
	@Column(name = "user_id")
    private Long userId;
	@Column(nullable = false)
    private Long accountId;
	
//	private int totalMobileNumberUpdated;
//	private int totalWhatsAppNumberUpdated;
//	private int totalRolesUpdated; 
//    private int totalBoothsUpdated; 
//    private int totalAddressUpdated;
//	
//	private int totalReligionUpdated;
//	
//	private int totalCasteUpdated;
//	
//	private int totalDobUpdated;
//	
//	private int totalPartyUpdated;
//	
//	private int totalVoterCreated;
//	
//	private int totalVoterUpdated;
//	
//	private LocalDateTime currentTimeStamp;
//	
//	  @PrePersist
//	    public void onSave() {
//		  totalMobileNumberUpdated=0;
//		  totalWhatsAppNumberUpdated = 0;
//	      totalRolesUpdated = 0;
//	      totalBoothsUpdated = 0;
//	      totalAddressUpdated = 0;
//		  totalReligionUpdated=0;
//		  totalCasteUpdated=0;
//		  totalDobUpdated=0;
//		  totalPartyUpdated=0;
//		  totalVoterCreated=0;
//		  totalVoterUpdated=0;
//		  currentTimeStamp= LocalDateTime.now();
//	  }
	@Column(name = "total_whatsapp_number_updated")
    private Long totalWhatsAppNumberUpdated;

    @Column(name = "total_roles_updated")
    private Long totalRolesUpdated;

    @Column(name = "total_booths_updated")
    private Long totalBoothsUpdated;

    @Column(name = "total_address_updated")
    private Long totalAddressUpdated;

    @Column(name = "total_mobile_number_updated")
    private Long totalMobileNumberUpdated;

    @Column(name = "total_religion_updated")
    private Long totalReligionUpdated;

    @Column(name = "total_caste_updated")
    private Long totalCasteUpdated;

    @Column(name = "total_dob_updated")
    private Long totalDobUpdated;

    @Column(name = "total_party_updated")
    private Long totalPartyUpdated;

    @Column(name = "total_language_updated")
    private Long totalLanguageUpdated;

    @Column(name = "total_voter_created")
    private Long totalVoterCreated;

    @Column(name = "total_voter_updated")
    private Long totalVoterUpdated;

    //@Column(name = "current_timestamp")
    private LocalDateTime currentTimeStamp;

    @PrePersist
    public void onSave() {
        totalMobileNumberUpdated = 0L;
        totalWhatsAppNumberUpdated = 0L;
        totalRolesUpdated = 0L;
        totalBoothsUpdated = 0L;
        totalAddressUpdated = 0L;
        totalReligionUpdated = 0L;
        totalCasteUpdated = 0L;
        totalDobUpdated = 0L;
        totalPartyUpdated = 0L;
        totalLanguageUpdated = 0L;
        totalVoterCreated = 1L;
        totalVoterUpdated = 0L;
        currentTimeStamp = LocalDateTime.now();
    }
	
}
