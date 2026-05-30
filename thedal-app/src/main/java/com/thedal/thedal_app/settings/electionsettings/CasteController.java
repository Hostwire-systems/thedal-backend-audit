//package com.thedal.thedal_app.settings.electionsettings;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.thedal.thedal_app.settings.electionsettings.dto.CasteRequest;
//
//@RestController
//@RequestMapping("/api/castes")
//public class CasteController {
//
//    @Autowired
//    private CasteService casteService;
//
//    @PostMapping
//    public ResponseEntity<?> createCaste(@RequestBody CasteRequest casteRequest) {
//        // Delegate the business logic to the service layer
//        CasteEntity casteEntity = casteService.createCaste(casteRequest);
//
//        return ResponseEntity.ok("Caste created successfully with id: " + casteEntity.getId());
//    }
//}
