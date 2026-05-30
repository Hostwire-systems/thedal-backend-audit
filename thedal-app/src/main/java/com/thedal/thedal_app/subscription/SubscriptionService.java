package com.thedal.thedal_app.subscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.subscription.dto.CreateModuleRequest;
import com.thedal.thedal_app.subscription.dto.GrantSubscriptionRequest;
import com.thedal.thedal_app.subscription.dto.SubscriptionModuleDto;
import com.thedal.thedal_app.subscription.dto.UserModuleAccessDto;
import com.thedal.thedal_app.subscription.dto.UserSubscriptionDto;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    
    private final SubscriptionModuleRepository moduleRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final UserRepo userRepository;
    
    // ===================== MODULE MANAGEMENT =====================
    
    @Transactional
    public SubscriptionModuleDto createModule(CreateModuleRequest request, String createdBy) {
        log.info("Creating module: {}", request.getModuleKey());
        
        if (moduleRepository.existsByModuleKey(request.getModuleKey())) {
            throw new IllegalArgumentException("Module with key " + request.getModuleKey() + " already exists");
        }
        
        SubscriptionModule module = new SubscriptionModule();
        module.setModuleKey(request.getModuleKey());
        module.setModuleName(request.getModuleName());
        module.setModuleDescription(request.getModuleDescription());
        module.setParentModuleId(request.getParentModuleId());
        module.setDisplayOrder(request.getDisplayOrder());
        module.setIconName(request.getIconName());
        module.setRoutePath(request.getRoutePath());
        module.setIsActive(request.getIsActive());
        module.setCreatedAt(LocalDateTime.now());
        module.setCreatedBy(createdBy);
        
        module = moduleRepository.save(module);
        log.info("Module created successfully: {}", module.getId());
        
        return mapToModuleDto(module);
    }
    
    @Transactional
    public SubscriptionModuleDto updateModule(Long moduleId, CreateModuleRequest request, String updatedBy) {
        log.info("Updating module: {}", moduleId);
        
        SubscriptionModule module = moduleRepository.findById(moduleId)
            .orElseThrow(() -> new IllegalArgumentException("Module not found with id: " + moduleId));
        
        // Check if module key is being changed and if it already exists
        if (!module.getModuleKey().equals(request.getModuleKey()) && 
            moduleRepository.existsByModuleKey(request.getModuleKey())) {
            throw new IllegalArgumentException("Module with key " + request.getModuleKey() + " already exists");
        }
        
        module.setModuleKey(request.getModuleKey());
        module.setModuleName(request.getModuleName());
        module.setModuleDescription(request.getModuleDescription());
        module.setParentModuleId(request.getParentModuleId());
        module.setDisplayOrder(request.getDisplayOrder());
        module.setIconName(request.getIconName());
        module.setRoutePath(request.getRoutePath());
        module.setIsActive(request.getIsActive());
        module.setUpdatedAt(LocalDateTime.now());
        module.setUpdatedBy(updatedBy);
        
        module = moduleRepository.save(module);
        log.info("Module updated successfully: {}", moduleId);
        
        return mapToModuleDto(module);
    }
    
    @Transactional(readOnly = true)
    public List<SubscriptionModuleDto> getAllModules() {
        log.info("Fetching all modules");
        List<SubscriptionModule> modules = moduleRepository.findAll();
        return modules.stream()
            .map(this::mapToModuleDto)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<SubscriptionModuleDto> getModulesHierarchy() {
        log.info("Fetching modules hierarchy");
        
        // Get all parent modules (modules without parent)
        List<SubscriptionModule> parentModules = moduleRepository.findByParentModuleIdIsNull();
        
        return parentModules.stream()
            .map(parent -> {
                SubscriptionModuleDto dto = mapToModuleDto(parent);
                // Get submodules
                List<SubscriptionModule> submodules = moduleRepository.findByParentModuleId(parent.getId());
                dto.setSubmodules(submodules.stream()
                    .map(this::mapToModuleDto)
                    .collect(Collectors.toList()));
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public SubscriptionModuleDto getModuleById(Long moduleId) {
        log.info("Fetching module by id: {}", moduleId);
        SubscriptionModule module = moduleRepository.findById(moduleId)
            .orElseThrow(() -> new IllegalArgumentException("Module not found with id: " + moduleId));
        return mapToModuleDto(module);
    }
    
    @Transactional
    public void deleteModule(Long moduleId) {
        log.info("Deleting module: {}", moduleId);
        
        // Check if module has any active subscriptions
        Long activeSubscriptions = subscriptionRepository.countActiveSubscriptionsByModuleId(moduleId);
        if (activeSubscriptions > 0) {
            throw new IllegalStateException("Cannot delete module with active subscriptions. " +
                "Revoke all subscriptions first.");
        }
        
        // Check if module has submodules
        List<SubscriptionModule> submodules = moduleRepository.findByParentModuleId(moduleId);
        if (!submodules.isEmpty()) {
            throw new IllegalStateException("Cannot delete module with submodules. " +
                "Delete submodules first.");
        }
        
        moduleRepository.deleteById(moduleId);
        log.info("Module deleted successfully: {}", moduleId);
    }
    
    // ===================== SUBSCRIPTION MANAGEMENT =====================
    
    @Transactional
    public List<UserSubscriptionDto> grantSubscriptions(GrantSubscriptionRequest request, String grantedBy) {
        log.info("Granting subscriptions to user: {} for {} modules", request.getUserId(), request.getModuleIds().size());
        
        UserEntity user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.getUserId()));
        
        LocalDateTime now = LocalDateTime.now();
        List<UserSubscription> subscriptions = request.getModuleIds().stream()
            .map(moduleId -> {
                SubscriptionModule module = moduleRepository.findById(moduleId)
                    .orElseThrow(() -> new IllegalArgumentException("Module not found with id: " + moduleId));
                
                // Check if subscription already exists
                Optional<UserSubscription> existingSubscription = 
                    subscriptionRepository.findByUserIdAndModuleId(request.getUserId(), moduleId);
                
                UserSubscription subscription;
                if (existingSubscription.isPresent()) {
                    // Update existing subscription
                    subscription = existingSubscription.get();
                    subscription.setHasAccess(true);
                    subscription.setExpiresAt(request.getExpiresAt());
                    subscription.setRevokedAt(null);
                    subscription.setRevokedBy(null);
                    subscription.setUpdatedAt(now);
                    log.info("Updated existing subscription for user: {} and module: {}", request.getUserId(), moduleId);
                } else {
                    // Create new subscription
                    subscription = new UserSubscription();
                    subscription.setUser(user);
                    subscription.setModule(module);
                    subscription.setHasAccess(true);
                    subscription.setGrantedAt(now);
                    subscription.setExpiresAt(request.getExpiresAt());
                    subscription.setGrantedBy(grantedBy);
                    subscription.setCreatedAt(now);
                    log.info("Created new subscription for user: {} and module: {}", request.getUserId(), moduleId);
                }
                
                return subscriptionRepository.save(subscription);
            })
            .collect(Collectors.toList());
        
        log.info("Subscriptions granted successfully");
        return subscriptions.stream()
            .map(this::mapToSubscriptionDto)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public void revokeSubscription(Long userId, Long moduleId, String revokedBy) {
        log.info("Revoking subscription for user: {} and module: {}", userId, moduleId);
        
        UserSubscription subscription = subscriptionRepository.findByUserIdAndModuleId(userId, moduleId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Subscription not found for user: " + userId + " and module: " + moduleId));
        
        subscription.setHasAccess(false);
        subscription.setRevokedAt(LocalDateTime.now());
        subscription.setRevokedBy(revokedBy);
        subscription.setUpdatedAt(LocalDateTime.now());
        
        subscriptionRepository.save(subscription);
        log.info("Subscription revoked successfully");
    }
    
    @Transactional(readOnly = true)
    public List<UserSubscriptionDto> getUserSubscriptions(Long userId) {
        log.info("Fetching subscriptions for user: {}", userId);
        
        List<UserSubscription> subscriptions = subscriptionRepository.findByUserId(userId);
        return subscriptions.stream()
            .map(this::mapToSubscriptionDto)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public UserModuleAccessDto getUserModuleAccess(Long userId) {
        log.info("Fetching module access for user: {}", userId);
        
        LocalDateTime now = LocalDateTime.now();
        List<UserSubscription> activeSubscriptions = 
            subscriptionRepository.findActiveSubscriptionsByUserId(userId, now);
        
        List<String> accessibleModuleKeys = activeSubscriptions.stream()
            .map(sub -> sub.getModule().getModuleKey())
            .collect(Collectors.toList());
        
        List<SubscriptionModuleDto> accessibleModules = activeSubscriptions.stream()
            .map(sub -> mapToModuleDto(sub.getModule()))
            .collect(Collectors.toList());
        
        UserModuleAccessDto access = new UserModuleAccessDto();
        access.setUserId(userId);
        access.setAccessibleModuleKeys(accessibleModuleKeys);
        access.setAccessibleModules(accessibleModules);
        
        return access;
    }
    
    @Transactional(readOnly = true)
    public boolean hasModuleAccess(Long userId, String moduleKey) {
        LocalDateTime now = LocalDateTime.now();
        Optional<UserSubscription> subscription = 
            subscriptionRepository.findActiveSubscriptionByUserIdAndModuleKey(userId, moduleKey, now);
        return subscription.isPresent();
    }
    
    @Transactional
    public void processExpiredSubscriptions() {
        log.info("Processing expired subscriptions");
        
        LocalDateTime now = LocalDateTime.now();
        List<UserSubscription> expiredSubscriptions = 
            subscriptionRepository.findExpiredSubscriptions(now);
        
        expiredSubscriptions.forEach(subscription -> {
            subscription.setHasAccess(false);
            subscription.setUpdatedAt(now);
        });
        
        subscriptionRepository.saveAll(expiredSubscriptions);
        log.info("Processed {} expired subscriptions", expiredSubscriptions.size());
    }
    
    // ===================== MAPPER METHODS =====================
    
    private SubscriptionModuleDto mapToModuleDto(SubscriptionModule module) {
        SubscriptionModuleDto dto = new SubscriptionModuleDto();
        dto.setId(module.getId());
        dto.setModuleKey(module.getModuleKey());
        dto.setModuleName(module.getModuleName());
        dto.setModuleDescription(module.getModuleDescription());
        dto.setParentModuleId(module.getParentModuleId());
        dto.setDisplayOrder(module.getDisplayOrder());
        dto.setIsActive(module.getIsActive());
        dto.setIconName(module.getIconName());
        dto.setRoutePath(module.getRoutePath());
        dto.setCreatedAt(module.getCreatedAt());
        dto.setUpdatedAt(module.getUpdatedAt());
        return dto;
    }
    
    private UserSubscriptionDto mapToSubscriptionDto(UserSubscription subscription) {
        UserSubscriptionDto dto = new UserSubscriptionDto();
        dto.setId(subscription.getId());
        dto.setUserId(subscription.getUser().getId());
        dto.setModuleId(subscription.getModule().getId());
        dto.setModuleKey(subscription.getModule().getModuleKey());
        dto.setModuleName(subscription.getModule().getModuleName());
        dto.setHasAccess(subscription.getHasAccess());
        dto.setGrantedAt(subscription.getGrantedAt());
        dto.setExpiresAt(subscription.getExpiresAt());
        dto.setGrantedBy(subscription.getGrantedBy());
        dto.setRevokedAt(subscription.getRevokedAt());
        dto.setRevokedBy(subscription.getRevokedBy());
        return dto;
    }
}
