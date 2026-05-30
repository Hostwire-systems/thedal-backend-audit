//package com.thedal.thedal_app.volunteer;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.thedal.thedal_app.account.AccountRepository;
//import com.thedal.thedal_app.user.UserEntity;
//import com.thedal.thedal_app.user.UserRepo;
//
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import lombok.Setter;
//
//@RestController
//@RequestMapping("/api/migration")
//@RequiredArgsConstructor
//public class AccountVolunteerMigrationController {
//
//    private final VolunteerRepository volunteerRepo;
//    private final UserRepo userRepo;
//
//    @PostMapping("/migrate-cadres")
//    public ResponseEntity<MigrationResult> migrateCadres() {
//        MigrationResult result = new MigrationResult();
//        result.setStartTime(LocalDateTime.now());
//        
//        try {
//            // Get all cadres needing migration
//            List<VolunteerEntity> cadres = volunteerRepo.findByAdminUserIdIsNullOrAdminUserId(0L);
//            result.setTotalCadres(cadres.size());
//            
//            for (VolunteerEntity cadre : cadres) {
//                // Find oldest admin in same account
//                UserEntity admin = userRepo.findFirstByAccountIdOrderByIdAsc(cadre.getAccountId())
//                    .orElse(userRepo.findById(1L).orElseThrow()); // Fallback to system admin
//                
//                cadre.setAdminUserId(admin.getId());
//                cadre.setModifiedTime(LocalDateTime.now());
//                volunteerRepo.save(cadre);
//                result.incrementUpdated();
//            }
//            
//            result.setStatus("COMPLETED");
//        } catch (Exception e) {
//            result.setStatus("FAILED: " + e.getMessage());
//        }
//        
//        result.setEndTime(LocalDateTime.now());
//        return ResponseEntity.ok(result);
//    }
//
//    @Getter @Setter
//    public static class MigrationResult {
//        private String status;
//        private LocalDateTime startTime;
//        private LocalDateTime endTime;
//        private int totalCadres;
//        private int updated;
//        
//        public void incrementUpdated() { updated++; }
//    }
//}