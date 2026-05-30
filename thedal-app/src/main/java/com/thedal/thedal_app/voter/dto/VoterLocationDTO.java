// package com.thedal.thedal_app.voter.dto;

// import lombok.AllArgsConstructor;
// import lombok.Data;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.Setter;

// @Getter
// @Setter
// @AllArgsConstructor
// @NoArgsConstructor
// @Data
// //@RequiredArgsConstructor
// public class VoterLocationDTO {
	
// 	private String voterId;
//     private Double latitude;
//     private Double longitude;

// }

package com.thedal.thedal_app.voter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class VoterLocationDTO {
    
    private String voterId;
    private Double voterLati;
    private Double voterLongi;
    // Add any other voter properties you need to include in the JSON
    // For example: voter name, address, etc.
}
