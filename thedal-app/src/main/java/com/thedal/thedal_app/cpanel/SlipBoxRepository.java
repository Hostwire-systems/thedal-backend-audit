package com.thedal.thedal_app.cpanel;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlipBoxRepository extends JpaRepository<SlipBoxEntity, Long> {
    List<SlipBoxEntity> findByAccountIdAndElectionId(Long accountId, Long electionId);
    Optional<SlipBoxEntity> findByIdAndAccountIdAndElectionId(Long id, Long accountId, Long electionId);
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    void deleteAllByIdInAndAccountIdAndElectionId(List<Long> ids, Long accountId, Long electionId);
    //boolean existsBySlipBoxIdAndElectionId(String slipBoxId, Long electionId);
	List<SlipBoxEntity> findBySlipBoxIdInAndElectionId(List<String> list, Long electionId);
	
	//boolean existsByAccountIdAndElectionIdAndIsDefault(Long accountId, Long electionId, boolean isDefault);
	/////////////////////
	
	boolean existsBySlipBoxIdAndAccountId(String slipBoxId, Long accountId);

    List<SlipBoxEntity> findByAccountId(Long accountId);

    Optional<SlipBoxEntity> findByIdAndAccountId(Long id, Long accountId);

    boolean existsBySlipBoxIdAndElectionId(String slipBoxId, Long electionId);

    boolean existsByAccountIdAndElectionIdAndIsDefault(Long accountId, Long electionId, boolean isDefault);

    void deleteAllByIdIn(List<Long> ids);
	
	
	
}