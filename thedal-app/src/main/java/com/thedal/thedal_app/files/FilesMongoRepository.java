package com.thedal.thedal_app.files;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface FilesMongoRepository extends MongoRepository<FilesMongo, Long> {
    
    // Find files by handler type and file ID
    List<FilesMongo> findByHandlerTypeAndHandlerFileId(HandlerType handlerType, Long handlerFileId);
    
    // Find files by handler type and file ID ordered by orderIndex
    List<FilesMongo> findByHandlerTypeAndHandlerFileIdOrderByOrderIndexAsc(HandlerType handlerType, Long handlerFileId);
    
    // Find active files by handler type and file ID
    List<FilesMongo> findByHandlerTypeAndHandlerFileIdAndIsActiveTrue(HandlerType handlerType, Long handlerFileId);
    
    // Find files with WhatsApp forward enabled
    List<FilesMongo> findByHandlerTypeAndHandlerFileIdAndWhatsappForwardTrue(HandlerType handlerType, Long handlerFileId);
    
    // Find specific file by ID, handler type and file ID
    Optional<FilesMongo> findByIdAndHandlerTypeAndHandlerFileId(Long id, HandlerType handlerType, Long handlerFileId);
    
    // Find files by IDs, handler type and file ID
    List<FilesMongo> findByIdInAndHandlerTypeAndHandlerFileId(List<Long> ids, HandlerType handlerType, Long handlerFileId);
    
    // Delete operations
    void deleteByIdIn(List<Long> ids);
    void deleteByHandlerTypeAndHandlerFileId(HandlerType handlerType, Long handlerFileId);
    
    // Find files by handler file IDs (for bulk operations)
    List<FilesMongo> findByHandlerTypeAndHandlerFileIdIn(HandlerType handlerType, List<Long> handlerFileIds);
    
    // Custom queries
    @Query("{'handlerType': ?0, 'handlerFileId': ?1, 'orderIndex': {$exists: true}}")
    List<FilesMongo> findByHandlerTypeAndHandlerFileIdWithOrderIndex(HandlerType handlerType, Long handlerFileId);
    
    @Query("{'handlerType': ?0, 'handlerFileId': ?1}")
    List<FilesMongo> findAllByHandlerTypeAndHandlerFileId(HandlerType handlerType, Long handlerFileId);
    
    // Count methods
    long countByHandlerTypeAndHandlerFileId(HandlerType handlerType, Long handlerFileId);
    long countByHandlerTypeAndHandlerFileIdAndIsActiveTrue(HandlerType handlerType, Long handlerFileId);
    
    // Max order index query
    @Query(value = "{'handlerType': ?0, 'handlerFileId': ?1}", fields = "{'orderIndex': 1}")
    List<FilesMongo> findOrderIndexesByHandlerTypeAndHandlerFileId(HandlerType handlerType, Long handlerFileId);
}
