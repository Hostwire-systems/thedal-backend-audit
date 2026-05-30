package com.thedal.thedal_app.voter;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.settings.electionsettings.Availability;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityRepository;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryEntity;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryRepository;
import com.thedal.thedal_app.settings.electionsettings.CasteEntity;
import com.thedal.thedal_app.settings.electionsettings.CasteRepository;
import com.thedal.thedal_app.settings.electionsettings.Party;
import com.thedal.thedal_app.settings.electionsettings.PartyRepository;
import com.thedal.thedal_app.settings.electionsettings.ReligionEntity;
import com.thedal.thedal_app.settings.electionsettings.ReligionRepository;
import com.thedal.thedal_app.settings.electionsettings.SubCasteEntity;
import com.thedal.thedal_app.settings.electionsettings.SubCasteRepository;

/**
 * Service for managing voter reference data creation with independent transactions.
 * Each getOrCreate method runs in a new transaction to prevent single failures from
 * aborting the parent bulk upload transaction.
 */
@Service
public class VoterReferenceDataService {
    
    private static final Logger log = LoggerFactory.getLogger(VoterReferenceDataService.class);
    
    @Autowired
    private ReligionRepository religionRepository;
    
    @Autowired
    private CasteRepository casteRepository;
    
    @Autowired
    private SubCasteRepository subCasteRepository;
    
    @Autowired
    private CasteCategoryRepository casteCategoryRepository;
    
    @Autowired
    private PartyRepository partyRepository;
    
    @Autowired
    private AvailabilityRepository availabilityRepository;
    
    /**
     * Get or create religion entity with independent transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReligionEntity getOrCreateReligion(String religionName, Long accountId, Long electionId, 
                                               Map<String, ReligionEntity> cache) {
        if (religionName == null || religionName.trim().isEmpty()) return null;
        
        String key = normalize(religionName);
        ReligionEntity religion = cache.get(key);
        
        if (religion == null) {
            // Check if exists in database
            religion = religionRepository.findByReligionNameAndAccountIdAndElectionId(religionName.trim(), accountId, electionId)
                    .orElse(null);
            
            if (religion == null) {
                // Create new religion
                try {
                    religion = new ReligionEntity();
                    religion.setReligionName(religionName.trim());
                    religion.setAccountId(accountId);
                    religion.setElectionId(electionId);
                    religion.setOrderIndex(0);
                    religion = religionRepository.save(religion);
                    log.info("Created new religion: {} for election {}", religionName, electionId);
                } catch (Exception e) {
                    log.warn("Failed to create religion {}: {}", religionName, e.getMessage());
                    // Try to fetch again in case another thread created it
                    religion = religionRepository.findByReligionNameAndAccountIdAndElectionId(religionName.trim(), accountId, electionId)
                            .orElse(null);
                }
            }
            
            if (religion != null) {
                cache.put(key, religion);
            }
        }
        return religion;
    }
    
    /**
     * Get or create caste entity with independent transaction
     * Caste MUST have a religion (database constraint)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CasteEntity getOrCreateCaste(String casteName, ReligionEntity religion, Long accountId, Long electionId,
                                        Map<String, CasteEntity> cache) {
        if (casteName == null || casteName.trim().isEmpty()) return null;
        if (religion == null) {
            log.warn("Cannot create caste {} without religion", casteName);
            return null;
        }
        
        String key = normalize(casteName) + "_" + religion.getId();
        CasteEntity caste = cache.get(key);
        
        if (caste == null) {
            // Check if exists in database
            List<CasteEntity> castes = casteRepository.findAllByReligion_AccountIdAndElectionId(accountId, electionId);
            caste = castes.stream()
                    .filter(c -> c.getCasteName().equalsIgnoreCase(casteName.trim()) && 
                                 c.getReligion() != null && 
                                 c.getReligion().getId().equals(religion.getId()))
                    .findFirst()
                    .orElse(null);
            
            if (caste == null) {
                try {
                    caste = new CasteEntity();
                    caste.setCasteName(casteName.trim());
                    caste.setAccountId(accountId);
                    caste.setElectionId(electionId);
                    caste.setOrderIndex(0);
                    caste.setReligion(religion); // CRITICAL: Set religion to avoid NULL constraint violation
                    caste = casteRepository.save(caste);
                    log.info("Created new caste: {} with religion {} for election {}", casteName, religion.getReligionName(), electionId);
                } catch (Exception e) {
                    log.warn("Failed to create caste {}: {}", casteName, e.getMessage());
                    // Try to fetch again
                    List<CasteEntity> retry = casteRepository.findAllByReligion_AccountIdAndElectionId(accountId, electionId);
                    caste = retry.stream()
                            .filter(c -> c.getCasteName().equalsIgnoreCase(casteName.trim()) && 
                                         c.getReligion() != null && 
                                         c.getReligion().getId().equals(religion.getId()))
                            .findFirst()
                            .orElse(null);
                }
            }
            
            if (caste != null) {
                cache.put(key, caste);
            }
        }
        return caste;
    }
    
    /**
     * Get or create sub-caste entity with independent transaction
     * SubCaste MUST have both caste and religion (database constraints)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SubCasteEntity getOrCreateSubCaste(String subCasteName, CasteEntity caste, ReligionEntity religion, 
                                              Long accountId, Long electionId, Map<String, SubCasteEntity> cache) {
        if (subCasteName == null || subCasteName.trim().isEmpty()) return null;
        if (caste == null || religion == null) {
            log.warn("Cannot create sub-caste {} without caste and religion", subCasteName);
            return null;
        }
        
        String key = normalize(subCasteName) + "_" + caste.getId() + "_" + religion.getId();
        SubCasteEntity subCaste = cache.get(key);
        
        if (subCaste == null) {
            // Check if exists in database
            List<SubCasteEntity> subCastes = subCasteRepository.findAllByReligion_AccountIdAndElectionIdOrderByUpdatedAtDescCreatedAtDesc(accountId, electionId);
            subCaste = subCastes.stream()
                    .filter(sc -> sc.getSubCasteName().equalsIgnoreCase(subCasteName.trim()) &&
                                  sc.getCaste() != null && sc.getCaste().getId().equals(caste.getId()) &&
                                  sc.getReligion() != null && sc.getReligion().getId().equals(religion.getId()))
                    .findFirst()
                    .orElse(null);
            
            if (subCaste == null) {
                try {
                    subCaste = new SubCasteEntity();
                    subCaste.setSubCasteName(subCasteName.trim());
                    subCaste.setAccountId(accountId);
                    subCaste.setElectionId(electionId);
                    subCaste.setOrderIndex(0);
                    subCaste.setCaste(caste);  // CRITICAL: Set caste to avoid NULL constraint violation
                    subCaste.setReligion(religion);  // CRITICAL: Set religion to avoid NULL constraint violation
                    subCaste = subCasteRepository.save(subCaste);
                    log.info("Created new sub-caste: {} with caste {} and religion {} for election {}", 
                             subCasteName, caste.getCasteName(), religion.getReligionName(), electionId);
                } catch (Exception e) {
                    log.warn("Failed to create sub-caste {}: {}", subCasteName, e.getMessage());
                    // Try to fetch again
                    List<SubCasteEntity> retry = subCasteRepository.findAllByReligion_AccountIdAndElectionIdOrderByUpdatedAtDescCreatedAtDesc(accountId, electionId);
                    subCaste = retry.stream()
                            .filter(sc -> sc.getSubCasteName().equalsIgnoreCase(subCasteName.trim()) &&
                                          sc.getCaste() != null && sc.getCaste().getId().equals(caste.getId()) &&
                                          sc.getReligion() != null && sc.getReligion().getId().equals(religion.getId()))
                            .findFirst()
                            .orElse(null);
                }
            }
            
            if (subCaste != null) {
                cache.put(key, subCaste);
            }
        }
        return subCaste;
    }
    
    /**
     * Get or create caste category entity with independent transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CasteCategoryEntity getOrCreateCasteCategory(String casteCategoryName, Long accountId, Long electionId,
                                                        Map<String, CasteCategoryEntity> cache) {
        if (casteCategoryName == null || casteCategoryName.trim().isEmpty()) return null;
        
        String key = normalize(casteCategoryName);
        CasteCategoryEntity casteCategory = cache.get(key);
        
        if (casteCategory == null) {
            // Check if exists in database
            casteCategory = casteCategoryRepository.findByCasteCategoryNameAndAccountIdAndElectionId(casteCategoryName.trim(), accountId, electionId)
                    .orElse(null);
            
            if (casteCategory == null) {
                try {
                    casteCategory = new CasteCategoryEntity();
                    casteCategory.setCasteCategoryName(casteCategoryName.trim());
                    casteCategory.setAccountId(accountId);
                    casteCategory.setElectionId(electionId);
                    casteCategory.setOrderIndex(0);
                    casteCategory = casteCategoryRepository.save(casteCategory);
                    log.info("Created new caste category: {} for election {}", casteCategoryName, electionId);
                } catch (Exception e) {
                    log.warn("Failed to create caste category {}: {}", casteCategoryName, e.getMessage());
                    // Try to fetch again
                    casteCategory = casteCategoryRepository.findByCasteCategoryNameAndAccountIdAndElectionId(casteCategoryName.trim(), accountId, electionId)
                            .orElse(null);
                }
            }
            
            if (casteCategory != null) {
                cache.put(key, casteCategory);
            }
        }
        return casteCategory;
    }
    
    /**
     * Get or create party entity with independent transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Party getOrCreateParty(String partyName, Long accountId, Long electionId,
                                  Map<String, Party> cache) {
        if (partyName == null || partyName.trim().isEmpty()) return null;
        
        String key = normalize(partyName);
        Party party = cache.get(key);
        
        if (party == null) {
            // Check if exists in database
            List<Party> parties = partyRepository.findAllByElectionIdAndAccountId(electionId, accountId);
            party = parties.stream()
                    .filter(p -> p.getPartyName().equalsIgnoreCase(partyName.trim()))
                    .findFirst()
                    .orElse(null);
            
            if (party == null) {
                try {
                    party = new Party();
                    party.setPartyName(partyName.trim());
                    party.setAccountId(accountId);
                    party.setElectionId(electionId);
                    party.setOrderIndex(0);
                    party = partyRepository.save(party);
                    log.info("Created new party: {} for election {}", partyName, electionId);
                } catch (Exception e) {
                    log.warn("Failed to create party {}: {}", partyName, e.getMessage());
                    // Try to fetch again
                    List<Party> retry = partyRepository.findAllByElectionIdAndAccountId(electionId, accountId);
                    party = retry.stream()
                            .filter(p -> p.getPartyName().equalsIgnoreCase(partyName.trim()))
                            .findFirst()
                            .orElse(null);
                }
            }
            
            if (party != null) {
                cache.put(key, party);
            }
        }
        return party;
    }
    
    /**
     * Get or create availability entity with independent transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Availability getOrCreateAvailability(String description, Long accountId, Long electionId,
                                                Map<String, Availability> cache) {
        if (description == null || description.trim().isEmpty()) return null;
        
        String key = normalize(description);
        Availability availability = cache.get(key);
        
        if (availability == null) {
            // Check if exists in database
            availability = availabilityRepository.findByDescriptionAndAccountIdAndElectionId(description.trim(), accountId, electionId)
                    .orElse(null);
            
            if (availability == null) {
                try {
                    availability = new Availability();
                    availability.setDescription(description.trim());
                    availability.setAvailabilityName(description.trim());
                    availability.setAccountId(accountId);
                    availability.setElectionId(electionId);
                    availability.setOrderIndex(0);
                    availability = availabilityRepository.save(availability);
                    log.info("Created new availability: {} for election {}", description, electionId);
                } catch (Exception e) {
                    log.warn("Failed to create availability {}: {}", description, e.getMessage());
                    // Try to fetch again
                    availability = availabilityRepository.findByDescriptionAndAccountIdAndElectionId(description.trim(), accountId, electionId)
                            .orElse(null);
                }
            }
            
            if (availability != null) {
                cache.put(key, availability);
            }
        }
        return availability;
    }
    
    /**
     * Get or create availability entity by category name with independent transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Availability getOrCreateAvailabilityByCategoryName(String categoryName, Long accountId, Long electionId,
                                                Map<String, Availability> cache) {
        if (categoryName == null || categoryName.trim().isEmpty()) return null;
        
        String key = normalize(categoryName);
        Availability availability = cache.get(key);
        
        if (availability == null) {
            // First, check if exists by category name
            availability = availabilityRepository.findByCategoryNameAndAccountIdAndElectionId(categoryName.trim(), accountId, electionId)
                    .orElse(null);
            
            if (availability == null) {
                // Fallback: check if exists by description (for existing records that don't have categoryName set)
                availability = availabilityRepository.findByDescriptionAndAccountIdAndElectionId(categoryName.trim(), accountId, electionId)
                        .orElse(null);
                
                if (availability != null) {
                    // Found by description - update categoryName if not set
                    if (availability.getCategoryName() == null || availability.getCategoryName().isEmpty()) {
                        availability.setCategoryName(categoryName.trim());
                        availability = availabilityRepository.save(availability);
                        log.info("Updated existing availability with category name: {} for election {}", categoryName, electionId);
                    }
                } else {
                    // Doesn't exist at all - create new
                    try {
                        availability = new Availability();
                        availability.setCategoryName(categoryName.trim());
                        availability.setDescription(categoryName.trim());
                        availability.setAvailabilityName(categoryName.trim());
                        availability.setAccountId(accountId);
                        availability.setElectionId(electionId);
                        availability.setOrderIndex(0);
                        availability = availabilityRepository.save(availability);
                        log.info("Created new availability by category name: {} for election {}", categoryName, electionId);
                    } catch (Exception e) {
                        log.warn("Failed to create availability by category name {}: {}", categoryName, e.getMessage());
                        // Final fallback: try to fetch by either method
                        availability = availabilityRepository.findByCategoryNameAndAccountIdAndElectionId(categoryName.trim(), accountId, electionId)
                                .orElse(availabilityRepository.findByDescriptionAndAccountIdAndElectionId(categoryName.trim(), accountId, electionId)
                                        .orElse(null));
                    }
                }
            }
            
            if (availability != null) {
                cache.put(key, availability);
            }
        }
        return availability;
    }
    
    /**
     * Normalize string for consistent cache key generation
     */
    private String normalize(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase();
    }
}
