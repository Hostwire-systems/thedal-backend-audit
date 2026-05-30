//package com.thedal.thedal_app.settings.electionsettings;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.thedal.thedal_app.settings.electionsettings.dto.ReligionRequest;
//
//@RestController
//@RequestMapping("/api/religions")
//public class ReligionController {
//
//    @Autowired
//    private ReligionService religionService;
//
//    @PostMapping
//    public ResponseEntity<?> createReligion(@RequestBody ReligionRequest religionRequest) {
//        // Delegate the business logic to the service layer
//        ReligionEntity religionEntity = religionService.createReligion(religionRequest);
//
//        return ResponseEntity.ok("Religion created successfully with id: " + religionEntity.getId());
//    }
//}