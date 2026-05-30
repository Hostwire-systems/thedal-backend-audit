package com.thedal.thedal_app.cpanel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.cpanel.dtos.SlipBoxDTO;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SlipBoxService {

    @Autowired
    private SlipBoxRepository slipBoxRepository;

    @Autowired
    private SlipBoxMongoRepository slipBoxMongoRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private RequestDetailsService requestDetails;

    private void validateElectionOwnership(Long electionId, Long accountId) {
        if (!electionRepository.existsByIdAndAccountId(electionId, accountId)) {
            log.error("Election ID {} not found for account {}", electionId, accountId);
            throw new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
    }

    private void validateSlipBoxId(String slipBoxId) {
        // Check if slipBoxId exists in cPanel (electionId = 0)
        if (!slipBoxRepository.existsBySlipBoxIdAndElectionId(slipBoxId, 0L)) {
            log.error("Slip box ID {} does not exist", slipBoxId);
            throw new ThedalException(ThedalError.INVALID_SLIP_BOX_ID, HttpStatus.BAD_REQUEST);
        }
    }

//    @Transactional
//    public ThedalResponse<SlipBoxDTO> createSlipBox(Long accountId, Long electionId, SlipBoxDTO slipBoxDTO) {
//        log.debug("Creating slip box for accountId={}, electionId={}", accountId, electionId);
//
//        validateElectionOwnership(electionId, accountId);
//        
//        // Check for duplicate slip box ID
//        if (slipBoxRepository.existsBySlipBoxIdAndElectionId(slipBoxDTO.getSlipBoxId(), electionId)) {
//            log.error("Slip box ID '{}' already exists for electionId {}", slipBoxDTO.getSlipBoxId(), electionId);
//            throw new ThedalException(ThedalError.SLIP_BOX_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
//        }
//
//        SlipBoxEntity entity = new SlipBoxEntity();
//        entity.setMobileNumber(slipBoxDTO.getMobileNumber());
//        entity.setSlipBoxName(slipBoxDTO.getSlipBoxName());
//        entity.setSlipBoxId(slipBoxDTO.getSlipBoxId());
//        entity.setAccountId(accountId);
//        entity.setElectionId(electionId);
//        entity.setIsDefault(false);
//
//        try {
//            // Save to PostgreSQL first
//            SlipBoxEntity savedEntity = slipBoxRepository.save(entity);
//            log.info("Slip box created in PostgreSQL with id={}", savedEntity.getId());
//
//            // Create MongoDB document
//            SlipBoxMongo mongoEntity = new SlipBoxMongo(savedEntity);
//            
//            try {
//                // Save to MongoDB
//                slipBoxMongoRepository.save(mongoEntity);
//                log.info("Slip box synced to MongoDB with id={}", mongoEntity.getId());
//            } catch (Exception mongoEx) {
//                log.error("Failed to save slip box to MongoDB: {}", mongoEx.getMessage());
//                
//                // Rollback PostgreSQL transaction
//                try {
//                    slipBoxRepository.deleteById(savedEntity.getId());
//                    log.info("Rolled back PostgreSQL slip box creation due to MongoDB failure");
//                } catch (Exception rollbackEx) {
//                    log.error("Failed to rollback PostgreSQL transaction: {}", rollbackEx.getMessage());
//                }
//                
//                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//
//            SlipBoxDTO responseDTO = new SlipBoxDTO(
//                    savedEntity.getId(),
//                    savedEntity.getMobileNumber(),
//                    savedEntity.getSlipBoxName(),
//                    savedEntity.getSlipBoxId(),
//                    savedEntity.getCreatedTime(),
//                    savedEntity.getModifiedTime(),
//                    savedEntity.isDefault()
//            );
//
//            return new ThedalResponse<>(ThedalSuccess.SLIP_BOX_CREATED, responseDTO);
//            
//        } catch (Exception ex) {
//            log.error("Failed to create slip box: {}", ex.getMessage());
//            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
    @Transactional
    public ThedalResponse<SlipBoxDTO> createSlipBox(Long accountId, SlipBoxDTO slipBoxDTO) {
        log.debug("Creating slip box for accountId={}", accountId);

        // Check for duplicate slip box ID
        if (slipBoxRepository.existsBySlipBoxIdAndAccountId(slipBoxDTO.getSlipBoxId(), accountId)) {
            log.error("Slip box ID '{}' already exists for accountId {}", slipBoxDTO.getSlipBoxId(), accountId);
            throw new ThedalException(ThedalError.SLIP_BOX_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        SlipBoxEntity entity = new SlipBoxEntity();
        entity.setMobileNumber(slipBoxDTO.getMobileNumber());
        entity.setSlipBoxName(slipBoxDTO.getSlipBoxName());
        entity.setSlipBoxId(slipBoxDTO.getSlipBoxId());
        entity.setAccountId(accountId);
        entity.setIsDefault(false);

        try {
            // Save to PostgreSQL first
            SlipBoxEntity savedEntity = slipBoxRepository.save(entity);
            log.info("Slip box created in PostgreSQL with id={}", savedEntity.getId());

            // Create MongoDB document
            SlipBoxMongo mongoEntity = new SlipBoxMongo(savedEntity);
            
            try {
                // Save to MongoDB
                slipBoxMongoRepository.save(mongoEntity);
                log.info("Slip box synced to MongoDB with id={}", mongoEntity.getId());
            } catch (Exception mongoEx) {
                log.error("Failed to save slip box to MongoDB: {}", mongoEx.getMessage());
                // Rollback PostgreSQL transaction
                try {
                    slipBoxRepository.deleteById(savedEntity.getId());
                    log.info("Rolled back PostgreSQL slip box creation due to MongoDB failure");
                } catch (Exception rollbackEx) {
                    log.error("Failed to rollback PostgreSQL transaction: {}", rollbackEx.getMessage());
                }
                throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            SlipBoxDTO responseDTO = new SlipBoxDTO(
                    savedEntity.getId(),
                    savedEntity.getMobileNumber(),
                    savedEntity.getSlipBoxName(),
                    savedEntity.getSlipBoxId(),
                    savedEntity.getCreatedTime(),
                    savedEntity.getModifiedTime(),
                    savedEntity.isDefault()
            );
            
    return new ThedalResponse<>(ThedalSuccess.SLIP_BOX_CREATED, responseDTO);
            
        } catch (Exception ex) {
            log.error("Failed to create slip box: {}", ex.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR );      
        }
    }

//    @Transactional
//    public ThedalResponse<List<SlipBoxDTO>> getSlipBoxes(Long accountId, Long electionId) {
//        log.debug("Fetching slip boxes for accountId={}, electionId={}", accountId, electionId);
//
//        validateElectionOwnership(electionId, accountId);
//
//        // Read from PostgreSQL for better performance
//        List<SlipBoxEntity> entities = slipBoxRepository.findByAccountIdAndElectionId(accountId, electionId);
//        // Sort by ID manually if needed
//        entities.sort((a, b) -> a.getId().compareTo(b.getId()));
//        List<SlipBoxDTO> dtos = entities.stream()
//                .map(entity -> new SlipBoxDTO(
//                        entity.getId(),
//                        entity.getMobileNumber(),
//                        entity.getSlipBoxName(),
//                        entity.getSlipBoxId(),
//                        null, // createdTime not needed for read operations
//                        null, // modifiedTime not needed for read operations
//                        entity.getIsDefault()
//                ))
//                .collect(Collectors.toList());
//
//        log.debug("Retrieved {} slip boxes from MongoDB", dtos.size());
//        return new ThedalResponse<>(ThedalSuccess.SLIP_BOXES_FOUND, dtos);
//    }
    @Transactional
    public ThedalResponse<List<SlipBoxDTO>> getSlipBoxes(Long accountId) {
        log.debug("Fetching slip boxes for accountId={}", accountId);

        // Read from PostgreSQL for better performance
        List<SlipBoxEntity> entities = slipBoxRepository.findByAccountId(accountId);
        // Sort by ID manually if needed
        entities.sort((a, b) -> a.getId().compareTo(b.getId()));
        List<SlipBoxDTO> dtos = entities.stream()
                .map(entity -> new SlipBoxDTO(
                        entity.getId(),
                        entity.getMobileNumber(),
                        entity.getSlipBoxName(),
                        entity.getSlipBoxId(),
                        null, // createdTime not needed for read operations
                        null, // modifiedTime not needed for read operations
                        entity.getIsDefault()
                ))
                .collect(Collectors.toList());

        log.debug("Retrieved {} slip boxes", dtos.size());
        return new ThedalResponse<>(ThedalSuccess.SLIP_BOXES_FOUND, dtos);
    }

//    @Transactional
//    public ThedalResponse<Void> deleteSlipBoxes(Long electionId, Long accountId, List<Long> slipBoxIds) {
//        validateElectionOwnership(electionId, accountId);
//
//        if (slipBoxIds.isEmpty()) {
//            throw new ThedalException(ThedalError.SLIP_BOX_IDS_REQUIRED, HttpStatus.BAD_REQUEST);
//        }
//
//        List<SlipBoxEntity> boxes = slipBoxRepository.findAllById(slipBoxIds);
//        List<Long> defaultSlipBoxes = boxes.stream()
//            .filter(SlipBoxEntity::isDefault)
//            .map(SlipBoxEntity::getId)
//            .toList();
//
//        if (!defaultSlipBoxes.isEmpty()) {
//            log.error("Attempt to delete default slip boxes with IDs: {}", defaultSlipBoxes);
//            throw new ThedalException(ThedalError.DEFAULT_SLIP_BOX_DELETION_NOT_ALLOWED, HttpStatus.BAD_REQUEST);
//        }
//
//        List<Long> unauthorized = boxes.stream()
//            .filter(b -> !b.getAccountId().equals(accountId) || !b.getElectionId().equals(electionId))
//            .map(SlipBoxEntity::getId)
//            .toList();
//
//        if (!unauthorized.isEmpty()) {
//            log.error("Unauthorized attempt to delete slip boxes: {}", unauthorized);
//            throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.FORBIDDEN);
//        }
//
//        try {
//            // Delete from PostgreSQL first
//            slipBoxRepository.deleteAllByIdInBatch(slipBoxIds);
//            log.info("Deleted {} slip boxes from PostgreSQL", slipBoxIds.size());
//
//            try {
//                // Delete from MongoDB
//                slipBoxMongoRepository.deleteByIdIn(slipBoxIds);
//                log.info("Deleted {} slip boxes from MongoDB", slipBoxIds.size());
//            } catch (Exception mongoEx) {
//                log.error("Failed to delete slip boxes from MongoDB: {}", mongoEx.getMessage());
//                // Note: PostgreSQL deletion already committed, log the MongoDB failure
//                // but don't throw exception as data consistency can be maintained via periodic sync
//            }
//
//            return new ThedalResponse<>(ThedalSuccess.SLIP_BOXES_DELETED, null);
//            
//        } catch (Exception ex) {
//            log.error("Failed to delete slip boxes: {}", ex.getMessage());
//            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
    @Transactional
    public ThedalResponse<Void> deleteSlipBoxes(Long accountId, List<Long> slipBoxIds) {
        if (slipBoxIds.isEmpty()) {
            throw new ThedalException(ThedalError.SLIP_BOX_IDS_REQUIRED, HttpStatus.BAD_REQUEST);
        }

        List<SlipBoxEntity> boxes = slipBoxRepository.findAllById(slipBoxIds);
        List<Long> defaultSlipBoxes = boxes.stream()
            .filter(SlipBoxEntity::isDefault)
            .map(SlipBoxEntity::getId)
            .toList();

        if (!defaultSlipBoxes.isEmpty()) {
            log.error("Attempt to delete default slip boxes with IDs: {}", defaultSlipBoxes);
            throw new ThedalException(ThedalError.DEFAULT_SLIP_BOX_DELETION_NOT_ALLOWED, HttpStatus.BAD_REQUEST);
        }

        List<Long> unauthorized = boxes.stream()
            .filter(b -> !b.getAccountId().equals(accountId))
            .map(SlipBoxEntity::getId)
            .toList();

        if (!unauthorized.isEmpty()) {
            log.error("Unauthorized attempt to delete slip boxes: {}", unauthorized);
            throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.FORBIDDEN);
        }

        try {
            // Delete from PostgreSQL first
            slipBoxRepository.deleteAllByIdIn(slipBoxIds); // Updated method name
            log.info("Deleted {} slip boxes from PostgreSQL", slipBoxIds.size());

            try {
                // Delete from MongoDB
                slipBoxMongoRepository.deleteByIdIn(slipBoxIds);
                log.info("Deleted {} slip boxes from MongoDB", slipBoxIds.size());
            } catch (Exception mongoEx) {
                log.error("Failed to delete slip boxes from MongoDB: {}", mongoEx.getMessage());
                // Note: PostgreSQL deletion already committed, log the MongoDB failure
            }

            return new ThedalResponse<>(ThedalSuccess.SLIP_BOXES_DELETED, null);
            
        } catch (Exception ex) {
            log.error("Failed to delete slip boxes: {}", ex.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
//    @Transactional
//    public void deleteSlipBox(Long electionId, Long accountId, Long slipBoxId) {
//        validateElectionOwnership(electionId, accountId);
//
//        Optional<SlipBoxEntity> existing = slipBoxRepository.findByIdAndAccountIdAndElectionId(slipBoxId, accountId, electionId);
//        if (existing.isEmpty()) {
//            throw new ThedalException(ThedalError.SLIP_BOX_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//        
//        if (existing.get().getIsDefault()) {
//            log.error("Attempt to delete default slip box with ID: {}", slipBoxId);
//            throw new ThedalException(ThedalError.DEFAULT_SLIP_BOX_DELETION_NOT_ALLOWED, HttpStatus.BAD_REQUEST);
//        }
//
//        try {
//            // Delete from PostgreSQL first
//            slipBoxRepository.deleteById(slipBoxId);
//            log.info("Deleted slip box {} from PostgreSQL", slipBoxId);
//
//            try {
//                // Delete from MongoDB
//                slipBoxMongoRepository.deleteById(slipBoxId);
//                log.info("Deleted slip box {} from MongoDB", slipBoxId);
//            } catch (Exception mongoEx) {
//                log.error("Failed to delete slip box {} from MongoDB: {}", slipBoxId, mongoEx.getMessage());
//                // Note: PostgreSQL deletion already committed, log the MongoDB failure
//            }
//            
//        } catch (Exception ex) {
//            log.error("Failed to delete slip box {}: {}", slipBoxId, ex.getMessage());
//            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
    @Transactional
    public void deleteSlipBox(Long accountId, Long slipBoxId) {
        Optional<SlipBoxEntity> existing = slipBoxRepository.findByIdAndAccountId(slipBoxId, accountId);
        if (existing.isEmpty()) {
            throw new ThedalException(ThedalError.SLIP_BOX_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        if (existing.get().getIsDefault()) {
            log.error("Attempt to delete default slip box with ID: {}", slipBoxId);
            throw new ThedalException(ThedalError.DEFAULT_SLIP_BOX_DELETION_NOT_ALLOWED, HttpStatus.BAD_REQUEST);
        }

        try {
            // Delete from PostgreSQL first
            slipBoxRepository.deleteById(slipBoxId);
            log.info("Deleted slip box {} from PostgreSQL", slipBoxId);

            try {
                // Delete from MongoDB
                slipBoxMongoRepository.deleteById(slipBoxId);
                log.info("Deleted slip box {} from MongoDB", slipBoxId);
            } catch (Exception mongoEx) {
                log.error("Failed to delete slip box {} from MongoDB: {}", slipBoxId, mongoEx.getMessage());
                // Note: PostgreSQL deletion already committed, log the MongoDB failure
            }
            
        } catch (Exception ex) {
            log.error("Failed to delete slip box {}: {}", slipBoxId, ex.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

	@Transactional
public void createDefaultSlipBoxesForExistingElections(Long accountId) {
    log.info("Starting migration to create default slip boxes for accountId={}", accountId);

    int pageSize = 100;
    int page = 0;
    List<ElectionEntity> elections;
    do {
        elections = electionRepository.findByAccountId(accountId, PageRequest.of(page, pageSize));
        for (ElectionEntity election : elections) {
            Long electionId = election.getId();
            if (slipBoxRepository.existsByAccountIdAndElectionIdAndIsDefault(accountId, electionId, true)) {
                log.debug("Default slip box already exists for electionId={}", electionId);
                continue;
            }
            
            try {
                SlipBoxEntity defaultSlipBox = new SlipBoxEntity();
                defaultSlipBox.setMobileNumber("0000000000");
                defaultSlipBox.setSlipBoxName("TEMP");
                defaultSlipBox.setSlipBoxId(UUID.randomUUID().toString());
                defaultSlipBox.setAccountId(accountId);
                defaultSlipBox.setElectionId(electionId);
                defaultSlipBox.setIsDefault(true);
                
                // Save to PostgreSQL first
                SlipBoxEntity savedSlipBox = slipBoxRepository.save(defaultSlipBox);
                String slipBoxName = String.format("TEAM%04d", savedSlipBox.getId() % 10000);
                savedSlipBox.setSlipBoxName(slipBoxName);
                savedSlipBox = slipBoxRepository.save(savedSlipBox);
                log.info("Created default slip box in PostgreSQL for electionId={}", electionId);
                
                try {
                    // Save to MongoDB
                    SlipBoxMongo mongoEntity = new SlipBoxMongo(savedSlipBox);
                    slipBoxMongoRepository.save(mongoEntity);
                    log.info("Synced default slip box to MongoDB for electionId={}", electionId);
                } catch (Exception mongoEx) {
                    log.error("Failed to sync default slip box to MongoDB for electionId={}: {}", electionId, mongoEx.getMessage());
                    // Continue with other elections
                }
                
            } catch (Exception ex) {
                log.error("Failed to create default slip box for electionId={}: {}", electionId, ex.getMessage());
                // Continue with other elections
            }
        }
        page++;
    } while (!elections.isEmpty());

    log.info("Migration completed for accountId={}", accountId);
}

//    @Transactional
//    public ThedalResponse<SlipBoxDTO> updateSlipBox(Long accountId, Long electionId, Long slipBoxId, SlipBoxDTO slipBoxDTO) {
//        log.debug("Updating slip box {} for accountId={}, electionId={}", slipBoxId, accountId, electionId);
//
//        validateElectionOwnership(electionId, accountId);
//
//        Optional<SlipBoxEntity> existingOpt = slipBoxRepository.findByIdAndAccountIdAndElectionId(slipBoxId, accountId, electionId);
//        if (existingOpt.isEmpty()) {
//            throw new ThedalException(ThedalError.SLIP_BOX_NOT_FOUND, HttpStatus.NOT_FOUND);
//        }
//
//        SlipBoxEntity existing = existingOpt.get();
//        
//        // Check for duplicate slip box ID if it's being changed
//        if (!existing.getSlipBoxId().equals(slipBoxDTO.getSlipBoxId()) && 
//            slipBoxRepository.existsBySlipBoxIdAndElectionId(slipBoxDTO.getSlipBoxId(), electionId)) {
//            log.error("Slip box ID '{}' already exists for electionId {}", slipBoxDTO.getSlipBoxId(), electionId);
//            throw new ThedalException(ThedalError.SLIP_BOX_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
//        }
//
//        // Update entity
//        existing.setMobileNumber(slipBoxDTO.getMobileNumber());
//        existing.setSlipBoxName(slipBoxDTO.getSlipBoxName());
//        existing.setSlipBoxId(slipBoxDTO.getSlipBoxId());
//
//        try {
//            // Update PostgreSQL first
//            SlipBoxEntity savedEntity = slipBoxRepository.save(existing);
//            log.info("Updated slip box in PostgreSQL with id={}", savedEntity.getId());
//
//            // Update MongoDB
//            SlipBoxMongo mongoEntity = new SlipBoxMongo(savedEntity);
//            
//            try {
//                slipBoxMongoRepository.save(mongoEntity);
//                log.info("Updated slip box in MongoDB with id={}", mongoEntity.getId());
//            } catch (Exception mongoEx) {
//                log.error("Failed to update slip box in MongoDB: {}", mongoEx.getMessage());
//                // Note: PostgreSQL update already committed, log the MongoDB failure
//            }
//
//            SlipBoxDTO responseDTO = new SlipBoxDTO(
//                    savedEntity.getId(),
//                    savedEntity.getMobileNumber(),
//                    savedEntity.getSlipBoxName(),
//                    savedEntity.getSlipBoxId(),
//                    savedEntity.getCreatedTime(),
//                    savedEntity.getModifiedTime(),
//                    savedEntity.isDefault()
//            );
//
//            return new ThedalResponse<>(ThedalSuccess.SLIP_BOX_UPDATED, responseDTO);
//            
//        } catch (Exception ex) {
//            log.error("Failed to update slip box: {}", ex.getMessage());
//            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
	@Transactional
    public ThedalResponse<SlipBoxDTO> updateSlipBox(Long accountId, Long slipBoxId, SlipBoxDTO slipBoxDTO) {
        log.debug("Updating slip box {} for accountId={}", slipBoxId, accountId);

        Optional<SlipBoxEntity> existingOpt = slipBoxRepository.findByIdAndAccountId(slipBoxId, accountId);
        if (existingOpt.isEmpty()) {
            throw new ThedalException(ThedalError.SLIP_BOX_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        SlipBoxEntity existing = existingOpt.get();
        
        // Check for duplicate slip box ID if it's being changed
        if (!existing.getSlipBoxId().equals(slipBoxDTO.getSlipBoxId()) && 
            slipBoxRepository.existsBySlipBoxIdAndAccountId(slipBoxDTO.getSlipBoxId(), accountId)) {
            log.error("Slip box ID '{}' already exists for accountId {}", slipBoxDTO.getSlipBoxId(), accountId);
            throw new ThedalException(ThedalError.SLIP_BOX_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        // Update entity
        existing.setMobileNumber(slipBoxDTO.getMobileNumber());
        existing.setSlipBoxName(slipBoxDTO.getSlipBoxName());
        existing.setSlipBoxId(slipBoxDTO.getSlipBoxId());

        try {
            // Update PostgreSQL first
            SlipBoxEntity savedEntity = slipBoxRepository.save(existing);
            log.info("Updated slip box in PostgreSQL with id={}", savedEntity.getId());

            // Update MongoDB
            SlipBoxMongo mongoEntity = new SlipBoxMongo(savedEntity);
            
            try {
                slipBoxMongoRepository.save(mongoEntity);
                log.info("Updated slip box in MongoDB with id={}", mongoEntity.getId());
            } catch (Exception mongoEx) {
                log.error("Failed to update slip box in MongoDB: {}", mongoEx.getMessage());
                // Note: PostgreSQL update already committed, log the MongoDB failure
            }

            SlipBoxDTO responseDTO = new SlipBoxDTO(
                    savedEntity.getId(),
                    savedEntity.getMobileNumber(),
                    savedEntity.getSlipBoxName(),
                    savedEntity.getSlipBoxId(),
                    savedEntity.getCreatedTime(),
                    savedEntity.getModifiedTime(),
                    savedEntity.isDefault()
            );

            return new ThedalResponse<>(ThedalSuccess.SLIP_BOX_UPDATED, responseDTO);
            
        } catch (Exception ex) {
            log.error("Failed to update slip box: {}", ex.getMessage());
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    
}