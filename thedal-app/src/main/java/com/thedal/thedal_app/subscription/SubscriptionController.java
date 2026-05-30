package com.thedal.thedal_app.subscription;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.subscription.dto.CreateModuleRequest;
import com.thedal.thedal_app.subscription.dto.GrantSubscriptionRequest;
import com.thedal.thedal_app.subscription.dto.SubscriptionModuleDto;
import com.thedal.thedal_app.subscription.dto.UserModuleAccessDto;
import com.thedal.thedal_app.subscription.dto.UserSubscriptionDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    // ===================== MODULE MANAGEMENT ENDPOINTS =====================
    
    @PostMapping("/modules")
    public ThedalResponse<SubscriptionModuleDto> createModule(
            @Valid @RequestBody CreateModuleRequest request,
            Authentication authentication) {
        log.info("Creating module: {}", request.getModuleKey());
        
        String createdBy = authentication != null ? authentication.getName() : "system";
        SubscriptionModuleDto module = subscriptionService.createModule(request, createdBy);
        
        return new ThedalResponse<>("Module created successfully", module, true);
    }
    
    @PutMapping("/modules/{moduleId}")
    public ThedalResponse<SubscriptionModuleDto> updateModule(
            @PathVariable Long moduleId,
            @Valid @RequestBody CreateModuleRequest request,
            Authentication authentication) {
        log.info("Updating module: {}", moduleId);
        
        String updatedBy = authentication != null ? authentication.getName() : "system";
        SubscriptionModuleDto module = subscriptionService.updateModule(moduleId, request, updatedBy);
        
        return new ThedalResponse<>("Module updated successfully", module, true);
    }
    
    @GetMapping("/modules")
    public ThedalResponse<List<SubscriptionModuleDto>> getAllModules(
            @RequestParam(required = false, defaultValue = "false") boolean hierarchy) {
        log.info("Fetching all modules, hierarchy: {}", hierarchy);
        
        List<SubscriptionModuleDto> modules = hierarchy 
            ? subscriptionService.getModulesHierarchy()
            : subscriptionService.getAllModules();
        
        return new ThedalResponse<>("Modules fetched successfully", modules, true);
    }
    
    @GetMapping("/modules/{moduleId}")
    public ThedalResponse<SubscriptionModuleDto> getModuleById(@PathVariable Long moduleId) {
        log.info("Fetching module: {}", moduleId);
        
        SubscriptionModuleDto module = subscriptionService.getModuleById(moduleId);
        return new ThedalResponse<>("Module fetched successfully", module, true);
    }
    
    @DeleteMapping("/modules/{moduleId}")
    public ThedalResponse<Void> deleteModule(@PathVariable Long moduleId) {
        log.info("Deleting module: {}", moduleId);
        
        subscriptionService.deleteModule(moduleId);
        return new ThedalResponse<>("Module deleted successfully", null, true);
    }
    
    // ===================== USER SUBSCRIPTION ENDPOINTS =====================
    
    @PostMapping("/users/grant")
    public ThedalResponse<List<UserSubscriptionDto>> grantSubscriptions(
            @Valid @RequestBody GrantSubscriptionRequest request,
            Authentication authentication) {
        log.info("Granting subscriptions to user: {}", request.getUserId());
        
        String grantedBy = authentication != null ? authentication.getName() : "system";
        List<UserSubscriptionDto> subscriptions = subscriptionService.grantSubscriptions(request, grantedBy);
        
        return new ThedalResponse<>("Subscriptions granted successfully", subscriptions, true);
    }
    
    @DeleteMapping("/users/{userId}/revoke/{moduleId}")
    public ThedalResponse<Void> revokeSubscription(
            @PathVariable Long userId,
            @PathVariable Long moduleId,
            Authentication authentication) {
        log.info("Revoking subscription for user: {} and module: {}", userId, moduleId);
        
        String revokedBy = authentication != null ? authentication.getName() : "system";
        subscriptionService.revokeSubscription(userId, moduleId, revokedBy);
        
        return new ThedalResponse<>("Subscription revoked successfully", null, true);
    }
    
    @GetMapping("/users/{userId}")
    public ThedalResponse<List<UserSubscriptionDto>> getUserSubscriptions(
            @PathVariable Long userId) {
        log.info("Fetching subscriptions for user: {}", userId);
        
        List<UserSubscriptionDto> subscriptions = subscriptionService.getUserSubscriptions(userId);
        return new ThedalResponse<>("User subscriptions fetched successfully", subscriptions, true);
    }
    
    @GetMapping("/users/{userId}/access")
    public ThedalResponse<UserModuleAccessDto> getUserModuleAccess(
            @PathVariable Long userId) {
        log.info("Fetching module access for user: {}", userId);
        
        UserModuleAccessDto access = subscriptionService.getUserModuleAccess(userId);
        return new ThedalResponse<>("User module access fetched successfully", access, true);
    }
    
    @GetMapping("/my-modules")
    public ThedalResponse<UserModuleAccessDto> getMyModules(Authentication authentication) {
        if (authentication == null) {
            return new ThedalResponse<>("User not authenticated", null, false);
        }
        
        // Extract user ID from authentication
        String username = authentication.getName();
        log.info("Fetching modules for authenticated user: {}", username);
        
        Long userId = extractUserIdFromAuthentication(authentication);
        
        UserModuleAccessDto access = subscriptionService.getUserModuleAccess(userId);
        return new ThedalResponse<>("Modules fetched successfully", access, true);
    }
    
    @GetMapping("/users/{userId}/has-access")
    public ThedalResponse<Boolean> hasModuleAccess(
            @PathVariable Long userId,
            @RequestParam String moduleKey) {
        log.info("Checking module access for user: {} and module: {}", userId, moduleKey);
        
        boolean hasAccess = subscriptionService.hasModuleAccess(userId, moduleKey);
        return new ThedalResponse<>("Access checked successfully", hasAccess, true);
    }
    
    // ===================== UTILITY METHODS =====================
    
    private Long extractUserIdFromAuthentication(Authentication authentication) {
        // This is a placeholder - implement based on your JWT/authentication setup
        // You might store userId in the JWT token and extract it here
        // For example: return ((UserDetails) authentication.getPrincipal()).getUserId();
        
        // Temporary implementation - you'll need to adjust this
        try {
            // If you store userId as a claim in JWT, extract it
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            log.error("Could not extract userId from authentication", e);
            throw new IllegalArgumentException("Invalid user authentication");
        }
    }
}
