package com.thedal.thedal_app.election;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateMongoRepository extends MongoRepository<TemplateMongo, Long> {

    // Find templates by account and election
    List<TemplateMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);

    // Find template by slip ID
    Optional<TemplateMongo> findBySlipId(String slipId);

    // Find template by account, election, and template name
    Optional<TemplateMongo> findByAccountIdAndElectionIdAndTemplateName(Long accountId, Long electionId, String templateName);

    // Find templates ordered by order index
    List<TemplateMongo> findByAccountIdAndElectionIdOrderByOrderIndexAsc(Long accountId, Long electionId);

    // Find active templates
    List<TemplateMongo> findByAccountIdAndElectionIdAndIsActive(Long accountId, Long electionId, Boolean isActive);

    // Find template by account, election, and template ID
    Optional<TemplateMongo> findByAccountIdAndElectionIdAndTemplateId(Long accountId, Long electionId, Long templateId);

    // Check if template exists by slip ID
    boolean existsBySlipId(String slipId);

    // Check if template exists by account, election, and template name
    boolean existsByAccountIdAndElectionIdAndTemplateName(Long accountId, Long electionId, String templateName);

    // Delete templates by account and election
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);

    // Delete templates by IDs
    void deleteByIdIn(List<Long> ids);

    // Delete templates by account, election, and template names
    void deleteByAccountIdAndElectionIdAndTemplateNameIn(Long accountId, Long electionId, List<String> templateNames);

    // Delete templates by account, election, excluding specific template name
    void deleteByAccountIdAndElectionIdAndTemplateNameNot(Long accountId, Long electionId, String templateName);

    // Count templates by account and election
    long countByAccountIdAndElectionId(Long accountId, Long electionId);

    // Find templates by election ID only
    List<TemplateMongo> findByElectionId(Long electionId);

    // Custom query to find templates with specific criteria
    @Query("{'accountId': ?0, 'electionId': ?1, 'isActive': ?2}")
    List<TemplateMongo> findActiveTemplatesByElection(Long accountId, Long electionId, Boolean isActive);

    // Find template by order index
    Optional<TemplateMongo> findByAccountIdAndElectionIdAndOrderIndex(Long accountId, Long electionId, Integer orderIndex);
}
