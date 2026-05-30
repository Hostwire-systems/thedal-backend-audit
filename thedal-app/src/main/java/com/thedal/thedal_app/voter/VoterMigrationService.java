//package com.thedal.thedal_app.voter;
//
//import java.util.List;
//import java.util.Objects;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.data.mongodb.core.query.Update;
//import org.springframework.stereotype.Service;
//
//import com.mongodb.client.result.UpdateResult;
//
//import jakarta.annotation.PostConstruct;
//
//@Service
//public class VoterMigrationService {
//    private static final Logger log = LoggerFactory.getLogger(VoterMigrationService.class);
//
//    @Autowired
//    private MongoTemplate mongoTemplate;
//
//    @Autowired
//    private VoterRepo voterRepository;
//
//    @PostConstruct
//    public void migrateVoterMongoFamilyId() {
//        // Phase 1: Add familyId: null to documents missing the field
//        try {
//            Query query = new Query(Criteria.where("familyId").exists(false));
//            Update update = new Update().set("familyId", null);
//            UpdateResult result = mongoTemplate.updateMulti(query, update, VoterMongo.class);
//            log.info("Migration Phase 1: Added familyId: null to {} VoterMongo documents", result.getModifiedCount());
//        } catch (Exception e) {
//            log.error("Failed to add familyId: null to VoterMongo documents: {}", e.getMessage());
//        }
//
//        // Phase 2: Sync familyId from PostgreSQL where mismatched
//        try {
//            List<VoterEntity> voters = voterRepository.findAll();
//            for (VoterEntity voter : voters) {
//                Query mongoQuery = new Query(Criteria.where("_id").is(voter.getId()));
//                VoterMongo voterMongo = mongoTemplate.findOne(mongoQuery, VoterMongo.class);
//                if (voterMongo != null && !Objects.equals(voterMongo.getFamilyId(), voter.getFamilyId())) {
//                    Update mongoUpdate = new Update().set("familyId", voter.getFamilyId());
//                    mongoTemplate.updateFirst(mongoQuery, mongoUpdate, VoterMongo.class);
//                    log.info("Synced familyId for voterId: {}, familyId: {}", voter.getVoterId(), voter.getFamilyId());
//                }
//            }
//            log.info("Migration Phase 2: Completed familyId sync from PostgreSQL to MongoDB");
//        } catch (Exception e) {
//            log.error("Failed to sync familyId from PostgreSQL to MongoDB: {}", e.getMessage());
//        }
//    }
//
//    
//}