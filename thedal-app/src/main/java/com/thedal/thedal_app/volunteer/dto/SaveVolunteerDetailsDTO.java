package com.thedal.thedal_app.volunteer.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveVolunteerDetailsDTO {

	//private String volunteerId;
    private String firstName;
    private String lastName;
    private String email;
    
    @NotBlank(message = "40319")
    @Pattern(
        regexp = "^[0-9]{10}$",
        message = "40352"
    )
    private String mobileNumber;
    @NotBlank(message = "40350")
    @Size(min = 8, message = "40351")
    private String password;
   // private String role;
    private AddressDTO address;
    //private String assignedBooth;
    private List<Long> assignedBooth;
    private List<Long> assignedFamilies;

    private String status;
    //private String photoUrl;
    private String remarks;
    private  String whatsAppNumber;
//    @NotNull(message = "40350")
//    @Min(value = 1, message = "40346")
//    private Long electionId;
    
    //private List<Long> electionBoothIds;
    private String gender;
    
    //private Long roleId;
    private String roleName;
    
     @JsonIgnore
    private Long electionId;

}
