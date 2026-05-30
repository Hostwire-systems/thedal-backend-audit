package com.thedal.thedal_app.role;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to initialize default static roles when an election is created.
 * These roles are created per account and are not editable by users.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoleInitializationService {

    private final RoleRepo roleRepo;
    private final ObjectMapper objectMapper;

    /**
     * Initialize 3 default static roles for an election/account:
     * 1. Booth Captain
     * 2. Family Captain
     * 3. Poll Captain
     * 
     * @param accountId The account ID for which roles are being created
     * @param electionId The election ID (for logging purposes)
     */
    @Transactional
    public void initializeDefaultRoles(Long accountId, Long electionId) {
        log.info("Initializing default static roles for accountId: {}, electionId: {}", accountId, electionId);

        try {
            createBoothCaptainRole(accountId);
            createFamilyCaptainRole(accountId);
            createPollCaptainRole(accountId);
            
            log.info("Successfully initialized 3 default roles for accountId: {}", accountId);
        } catch (Exception e) {
            log.error("Error initializing default roles for accountId: {}, electionId: {}, error: {}", 
                    accountId, electionId, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize default roles", e);
        }
    }

    /**
     * Create Booth Captain role if it doesn't already exist for the account
     */
    private void createBoothCaptainRole(Long accountId) {
        String roleName = "Booth Captain";
        
        // Check if role already exists for this account
        Optional<Role> existingRole = roleRepo.findByRoleNameAndAccountId(roleName, accountId);
        if (existingRole.isPresent()) {
            log.info("Role '{}' already exists for accountId: {}, skipping creation", roleName, accountId);
            return;
        }

        Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("voterList", Arrays.asList("R", "U"));
        permissions.put("voterMap", Arrays.asList("R", "U"));
        permissions.put("newVoter", Arrays.asList("C", "R", "U", "D"));

        String description = "Manages voter engagement at grassroots level and collects voter data";

        saveRole(roleName, permissions, description, accountId);
        log.info("Created Booth Captain role for accountId: {}", accountId);
    }

    /**
     * Create Family Captain role if it doesn't already exist for the account
     */
    private void createFamilyCaptainRole(Long accountId) {
        String roleName = "Family Captain";
        
        // Check if role already exist for this account
        Optional<Role> existingRole = roleRepo.findByRoleNameAndAccountId(roleName, accountId);
        if (existingRole.isPresent()) {
            log.info("Role '{}' already exists for accountId: {}, skipping creation", roleName, accountId);
            return;
        }

        Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("family", Arrays.asList("R"));
        permissions.put("familyPollStatus", Arrays.asList("R"));

        String description = "Manages assigned families and tracks polling on election day";

        saveRole(roleName, permissions, description, accountId);
        log.info("Created Family Captain role for accountId: {}", accountId);
    }

    /**
     * Create Poll Captain role if it doesn't already exist for the account
     */
    private void createPollCaptainRole(Long accountId) {
        String roleName = "Poll Captain";
        
        // Check if role already exists for this account
        Optional<Role> existingRole = roleRepo.findByRoleNameAndAccountId(roleName, accountId);
        if (existingRole.isPresent()) {
            log.info("Role '{}' already exists for accountId: {}, skipping creation", roleName, accountId);
            return;
        }

        Map<String, List<String>> permissions = new HashMap<>();
        permissions.put("polldayVote", Arrays.asList("R", "U"));

        String description = "Manages polling booth and tracks real-time voting on election day";

        saveRole(roleName, permissions, description, accountId);
        log.info("Created Poll Captain role for accountId: {}", accountId);
    }

    /**
     * Save a role to the database using native query
     */
    private void saveRole(String roleName, Map<String, List<String>> rolePermission, String description, Long accountId) {
        try {
            // Convert permission map to JSON string
            String permissionJson = objectMapper.writeValueAsString(rolePermission);
            
            // For now, set permission integer to 0 (not using bitwise permissions for these roles)
            Integer permission = 0;

            roleRepo.insertRole(roleName, permission, permissionJson, description, accountId);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize permissions for role: {}", roleName, e);
            throw new RuntimeException("Failed to create role: " + roleName, e);
        }
    }
}
