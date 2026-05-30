//package com.thedal.thedal_app.voter;
//
//import java.util.List;
//import java.util.Optional;
//
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.mongodb.repository.MongoRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface VoterMongoRepo extends MongoRepository<VoterEntityMongo,String>{
//
//    Optional<VoterEntityMongo> findByVoterIdAndElectionId(String voterId, Long electionId);
//    Optional<VoterEntityMongo> findByEpicNumber(String epicNumber);
//    Optional<VoterEntityMongo> findByAccountIdAndElectionIdAndVoterId(Long accountId, Long electionId, String voterId);
//    Page<VoterEntityMongo> findByAccountIdAndVoterIdAndElectionId(Long accountId, String voterId, Long electionId,
//			Pageable pageable);
//
//	Page<VoterEntityMongo> findByAccountIdAndElectionIdAndBoothNumber(Long accountId, Long electionId, Integer boothNumber,
//			Pageable pageable);
//
//	Page<VoterEntityMongo> findByElectionIdAndBoothNumberAndAccountId(Long electionId, Long boothNumber, Long accountId,
//			Pageable pageable);
//
//   Page<VoterEntityMongo> findByAccountIdAndElectionId(Long accountId, Long electionId, Pageable pageable);
//   Optional<VoterEntityMongo> findByVoterIdAndElectionIdAndAccountId(String voterId, Long electionId, Long accountId);
//
//   List<VoterEntityMongo> findByElectionIdAndAccountId(Long electionId, Long accountId);
//   Page<VoterEntityMongo> findByElectionIdAndAccountId(Long electionId, Long accountId, Pageable pageable);
//   List<VoterEntityMongo> findAllByVoterIdInAndElectionId(List<String> voterIds, Long electionId);
//   Optional<VoterEntityMongo> findByEpicNumberAndElectionIdAndAccountId(String epicNumber, Long electionId, Long accountId);
//Page<VoterEntityMongo> findByAccountIdAndElectionIdAndEpicNumber(Long accountId, Long electionId, String epicNumber,
//		Pageable pageable);
//   
//          
//}
