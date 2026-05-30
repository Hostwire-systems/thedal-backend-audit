//package com.thedal.thedal_app.settings.electionsettings;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.thedal.thedal_app.settings.electionsettings.dto.SubCasteRequest;
//
//@RestController
//@RequestMapping("/api/subcastes")
//public class SubCasteController {
//
//    @Autowired
//    private SubCasteService subCasteService;
//
//    @PostMapping
//    public ResponseEntity<?> createSubCaste(@RequestBody SubCasteRequest subCasteRequest) {
//        // Delegate the business logic to the service layer
//        SubCasteEntity subCasteEntity = subCasteService.createSubCaste(subCasteRequest);
//
//        return ResponseEntity.ok("SubCaste created successfully with id: " + subCasteEntity.getId());
//    }
//}