package com.thedal.thedal_app.volunteer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.account.AccountRepository;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
public class CadreMigrationController {

    private final VolunteerRepository volunteerRepo;
    private final UserRepo userRepo;
    private final AccountRepository accountRepo;
    private final JdbcTemplate jdbcTemplate;
    private final MongoVolunteerRepository mongoVolunteerRepository;

    @PostMapping("/migrate-account-cadres/{accountId}")
    @Transactional(rollbackOn = {Exception.class})
    public ResponseEntity<MigrationResult> migrateAccountCadres(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        
        MigrationResult result = new MigrationResult();
        result.setAccountId(accountId);
        result.setStartTime(LocalDateTime.now());
        result.setDryRun(dryRun);

        try {
            // Verify account exists
            accountRepo.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

            // Find the oldest ADMIN user (role_id=1) for this account
            Optional<UserEntity> adminUserOpt = userRepo.findFirstByAccountEntityIdAndRoleIdOrderByIdAsc(accountId, 1L);
            if (!adminUserOpt.isPresent()) {
                throw new IllegalStateException("No admin user found for account ID: " + accountId);
            }
            Long adminUserId = adminUserOpt.get().getId();
            result.setAdminUserId(adminUserId);

            // Get all cadres with missing or invalid admin_user_id for this account
            List<VolunteerEntity> cadres = volunteerRepo.findByAccountIdAndAdminUserIdIsNullOrAdminUserId(accountId, 0L);
            result.setTotalCadres(cadres.size());

            if (!cadres.isEmpty()) {
                if (!dryRun) {
                    // Batch update volunteers table
                    jdbcTemplate.batchUpdate(
                        "UPDATE volunteers SET admin_user_id = ?, modified_time = ? WHERE id = ?",
                        new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement ps, int i) throws SQLException {
                                VolunteerEntity cadre = cadres.get(i);
                                ps.setLong(1, adminUserId);
                                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                                ps.setLong(3, cadre.getId());
                            }
                            @Override
                            public int getBatchSize() {
                                return cadres.size();
                            }
                        }
                    );

                    // Update MongoDB
                    for (VolunteerEntity cadre : cadres) {
                        cadre.setAdminUserId(adminUserId);
                        cadre.setModifiedTime(LocalDateTime.now());
                        MongoVolunteer mongoVolunteer = mapToMongoVolunteer(cadre);
                        mongoVolunteerRepository.save(mongoVolunteer);
                    }
                }
                result.setUpdated(cadres.size());
            }

            // Ensure account_id in volunteers matches account_entity_id in _user
            List<VolunteerEntity> mismatchedCadres = volunteerRepo.findByAccountIdMismatch(accountId);
            if (!mismatchedCadres.isEmpty()) {
                if (!dryRun) {
                    jdbcTemplate.batchUpdate(
                        "UPDATE volunteers SET account_id = ? WHERE id = ?",
                        new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement ps, int i) throws SQLException {
                                VolunteerEntity cadre = mismatchedCadres.get(i);
                                ps.setLong(1, accountId);
                                ps.setLong(2, cadre.getId());
                            }
                            @Override
                            public int getBatchSize() {
                                return mismatchedCadres.size();
                            }
                        }
                    );

                    // Update MongoDB for mismatched cadres
                    for (VolunteerEntity cadre : mismatchedCadres) {
                        cadre.setAccountId(accountId);
                        MongoVolunteer mongoVolunteer = mapToMongoVolunteer(cadre);
                        mongoVolunteerRepository.save(mongoVolunteer);
                    }
                }
                result.setUpdated(result.getUpdated() + mismatchedCadres.size());
            }

            result.setStatus("COMPLETED");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.setStatus("FAILED: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } finally {
            result.setEndTime(LocalDateTime.now());
        }
    }
    
    private MongoVolunteer mapToMongoVolunteer(VolunteerEntity volunteerEntity) {
        MongoVolunteer mongoVolunteer = new MongoVolunteer();
        mongoVolunteer.setId(volunteerEntity.getId().toString());
        mongoVolunteer.setLastName(volunteerEntity.getLastName());
        mongoVolunteer.setEmail(volunteerEntity.getEmail());
        mongoVolunteer.setMobileNumber(volunteerEntity.getMobileNumber());
        mongoVolunteer.setAssignedBooth(volunteerEntity.getAssignedBooth());
        mongoVolunteer.setStatus(volunteerEntity.getStatus());
        mongoVolunteer.setPhotoUrl(volunteerEntity.getPhotoUrl());
        mongoVolunteer.setRemarks(volunteerEntity.getRemarks());
        mongoVolunteer.setVolunteerAddress(volunteerEntity.getVolunteerAddress());
        mongoVolunteer.setAccountId(volunteerEntity.getAccountId());
        mongoVolunteer.setCreatedTime(volunteerEntity.getCreatedTime());
        mongoVolunteer.setModifiedTime(volunteerEntity.getModifiedTime());
        mongoVolunteer.setUserEntity(volunteerEntity.getUserEntity());
        mongoVolunteer.setElectionEntity(volunteerEntity.getElectionEntity());
        mongoVolunteer.setWhatsAppNumber(volunteerEntity.getWhatsAppNumber());
        mongoVolunteer.setGender(volunteerEntity.getGender());
        mongoVolunteer.setRoleId(volunteerEntity.getRoleId());
        mongoVolunteer.setAdminUserId(volunteerEntity.getAdminUserId());
        return mongoVolunteer;
    }
    
    @Getter @Setter
    public static class MigrationResult {
        private Long accountId;
        private Long adminUserId;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean dryRun;
        private int totalCadres;
        private int updated;
    }
}