//package com.thedal.thedal_app.settings.electionsettings;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.thedal.thedal_app.general.RequestDetailsService;
//import com.thedal.thedal_app.response.ThedalResponse;
//import com.thedal.thedal_app.response.ThedalSuccess;
//import com.thedal.thedal_app.settings.electionsettings.dto.GetReligionCasteSubCasteDTO;
//import com.thedal.thedal_app.settings.electionsettings.dto.ReligionCasteSubCasteDTO;
//import com.thedal.thedal_app.settings.electionsettings.dto.ReligionCasteSubCasteUpdateDTO;
//import com.thedal.thedal_app.thedal_exception.ThedalError;
//import com.thedal.thedal_app.thedal_exception.ThedalException;
//
//import io.swagger.v3.oas.annotations.Operation;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//@RestController
//@RequestMapping("/all-religions")
//@Slf4j
//@RequiredArgsConstructor
//public class ElectionSettingsController {
//
//	@Autowired
//    private ReligionCasteSubCasteService service;
//	
//	@Autowired
//    private RequestDetailsService requestDetails;
//
//    @PostMapping
//    @Operation(summary = "Create Religion, Caste, and SubCaste", description = "Creates a new religion along with its associated castes and sub-castes", tags = { "Religion Management" })
//    public ThedalResponse<Void> createReligionCasteSubcaste(@RequestBody ReligionCasteSubCasteDTO dto) {
//        service.createReligionCasteSubcaste(dto);
//        return new ThedalResponse<>(ThedalSuccess.RELIGION_CASTE_SUBCASTE_CREATED);
//    }
//    
//    
////    @GetMapping
////    @Operation(summary = "Get All Religion, Caste, and SubCaste Hierarchies", description = "Retrieves all religions along with their associated castes and sub-castes", tags = { "Religion Management" })
////    public ThedalResponse<List<GetReligionCasteSubCasteDTO>> getAllReligionCasteSubcaste() {
////        List<GetReligionCasteSubCasteDTO> religionHierarchy = service.getAllReligionCasteSubcaste();
////        return new ThedalResponse<>(ThedalSuccess.RELIGION_CASTE_SUBCASTE_DATE_RETRIEVED, religionHierarchy);
////    }
//    
////    @GetMapping("/{religionId}")
////    @Operation(
////        summary = "Get Religion, Caste, and SubCaste Hierarchy by Religion ID",
////        description = "Retrieves a religion along with its associated castes and sub-castes based on religion ID",
////        tags = { "Religion Management" }
////    )
////    public ThedalResponse<GetReligionCasteSubCasteDTO> getReligionCasteSubcasteByReligionId(
////        @PathVariable Long religionId) {
////    	
////    	Long accountId = requestDetails.getCurrentAccountId();
////        if (accountId == null) {
////            log.error("Account ID not found, unauthorized access.");
////            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
////        }
////    	
////        GetReligionCasteSubCasteDTO religionHierarchy = service.getReligionCasteSubcasteByReligionId(religionId, accountId);
////        return new ThedalResponse<>(ThedalSuccess.RELIGION_CASTE_SUBCASTE_DATE_RETRIEVED, religionHierarchy);
////    }
//
//    
////    @PutMapping
////    @Operation(summary = "Update Religion, Caste, and SubCaste Hierarchies", description = "Updates religion, castes, and sub-castes based on the provided details", tags = { "Religion Management" })
////    public ThedalResponse<Void> updateReligionCasteSubcaste(@RequestBody ReligionCasteSubCasteUpdateDTO dto) {
////        service.updateReligionCasteSubcaste(dto);
////        return new ThedalResponse<>(ThedalSuccess.RELIGION_HIERARCHY_UPDATED);
////    }
//    @PutMapping("/{religionId}")
//    @Operation(summary = "Update Religion, Caste, and SubCaste Hierarchies", description = "Updates religion, castes, and sub-castes based on the provided details", tags = { "Religion Management" })
//    public ThedalResponse<Void> updateReligionCasteSubcaste(
//        @PathVariable Long religionId, 
//        @RequestBody ReligionCasteSubCasteUpdateDTO dto) {
//    	
//    	Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//        
//        service.updateReligionCasteSubcaste(religionId, dto, accountId);
//        return new ThedalResponse<>(ThedalSuccess.RELIGION_HIERARCHY_UPDATED);
//    }
//    
//    @DeleteMapping("/{religionId}")
//    @Operation(summary = "Delete Religion, Castes, and SubCastes", description = "Deletes the specified religion along with its associated castes and sub-castes", tags = { "Religion Management" })
//    public ThedalResponse<Void> deleteReligionCasteSubcaste(@PathVariable Long religionId) {
//    	Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//    	
//    	service.deleteReligionCasteSubcaste(religionId, accountId);
//        return new ThedalResponse<>(ThedalSuccess.RELIGION_HIERARCHY_DELETED);
//    }
//	
//}
