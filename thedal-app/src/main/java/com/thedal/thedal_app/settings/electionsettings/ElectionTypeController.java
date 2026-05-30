//package com.thedal.thedal_app.settings.electionsettings;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.thedal.thedal_app.response.ThedalResponse;
//import com.thedal.thedal_app.response.ThedalSuccess;
//import com.thedal.thedal_app.settings.electionsettings.dto.ElectionTypeRequest;
//
//import io.swagger.v3.oas.annotations.Operation;
//import jakarta.validation.Valid;
//
//@RestController
//@RequestMapping("/election-types")
//public class ElectionTypeController {
//	
//	@Autowired
//	private ElectionTypeService electionTypeService;
//	    
//	@Operation(summary = "Create a new Election Type", description = "Saves a new election type")
//    @PostMapping("/election-types")
//    public ThedalResponse<ElectionType> createElectionType(@RequestBody @Valid ElectionTypeRequest electionTypeRequest) {
//        ThedalResponse<ElectionType> response = electionTypeService.createElectionType(electionTypeRequest);
//        return new ThedalResponse<>(ThedalSuccess.ELECTION_TYPE_CREATED, response.getData());
//    }    
//
//
//}
