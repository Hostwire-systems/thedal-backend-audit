package com.thedal.thedal_app.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CadreOverviewResponseDTO {

	private int noOfCadre;
	
	private int cadreLogged;
	
	private int cadreNotLogged;
	
	//private Long totalMobileNumberUpdated;
	
    private Long totalWhatsappNumberUpdated;
    
    private Long totalRolesUpdated;
    
    private Long totalBoothsUpdated;
    
    private Long totalAddressUpdated;
    
	private int activeCadreCount;  
    private int inactiveCadreCount;
    
    private int maleCadreCount;
    private int femaleCadreCount;
    private int otherCadreCount;

	
}
