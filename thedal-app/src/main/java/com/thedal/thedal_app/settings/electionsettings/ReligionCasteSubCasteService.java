//package com.thedal.thedal_app.settings.electionsettings;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//
//import com.thedal.thedal_app.general.RequestDetailsService;
//import com.thedal.thedal_app.response.ThedalResponse;
//import com.thedal.thedal_app.response.ThedalSuccess;
//import com.thedal.thedal_app.settings.electionsettings.dto.GetReligionCasteSubCasteDTO;
//import com.thedal.thedal_app.settings.electionsettings.dto.ReligionCasteSubCasteDTO;
//import com.thedal.thedal_app.settings.electionsettings.dto.ReligionCasteSubCasteUpdateDTO;
//import com.thedal.thedal_app.thedal_exception.ThedalError;
//import com.thedal.thedal_app.thedal_exception.ThedalException;
//
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class ReligionCasteSubCasteService {
//	
//	@Autowired
//    private ReligionRepository religionRepository;
//
//    @Autowired
//    private CasteRepository casteRepository;
//
//    @Autowired
//    private SubCasteRepository subCasteRepository;
//    
//    @Autowired
//    private RequestDetailsService requestDetails;
//
//    @Transactional
//    public ThedalResponse<Void> createReligionCasteSubcaste(ReligionCasteSubCasteDTO dto) {
//    	Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//        
//        ReligionEntity religion = new ReligionEntity();
//        religion.setReligionName(dto.getReligionName());
//        religion.setAccountId(accountId);
//
//        List<CasteEntity> castes = new ArrayList<>();
//
//        for (ReligionCasteSubCasteDTO.CasteDTO casteDTO : dto.getCastes()) {
//        	CasteEntity caste = new CasteEntity();
//            caste.setCasteName(casteDTO.getCasteName());
//            caste.setReligion(religion);
//            caste.setAccountId(accountId);
//
//            List<SubCasteEntity> subCastes = new ArrayList<>();
//            for (String subCasteName : casteDTO.getSubCasteNames()) {
//            	SubCasteEntity subCaste = new SubCasteEntity();
//                subCaste.setSubCasteName(subCasteName);
//                subCaste.setCaste(caste);
//                subCaste.setReligion(religion);
//                subCaste.setAccountId(accountId);
//                subCastes.add(subCaste);
//            }
//            caste.setSubCastes((List<SubCasteEntity>) subCastes);
//            castes.add(caste);
//        }
//
//        religion.setCastes((List<CasteEntity>) castes);
//        religionRepository.save(religion);
//        
//        return new ThedalResponse<>(ThedalSuccess.RELIGION_CASTE_SUBCASTE_CREATED);
//    }
//    
////    public List<GetReligionCasteSubCasteDTO> getAllReligionCasteSubcaste() {
////    	Long accountId = requestDetails.getCurrentAccountId();
////        if (accountId == null) {
////            log.error("Account ID not found, unauthorized access.");
////            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
////        }
////    	
////        //List<ReligionEntity> religions = religionRepository.findAll();
////        List<ReligionEntity> religions = religionRepository.findByAccountId(accountId);
////
////        return religions.stream().map(religion -> {
////        	GetReligionCasteSubCasteDTO religionDTO = new GetReligionCasteSubCasteDTO();
////            religionDTO.setReligionId(religion.getId());
////            religionDTO.setReligionName(religion.getReligionName());
////
////            List<GetReligionCasteSubCasteDTO.CasteDTO> casteDTOs = religion.getCastes().stream().map(caste -> {
////            	GetReligionCasteSubCasteDTO.CasteDTO casteDTO = new GetReligionCasteSubCasteDTO.CasteDTO();
////                casteDTO.setCasteId(caste.getId());
////                casteDTO.setCasteName(caste.getCasteName());
////
////                List<GetReligionCasteSubCasteDTO.CasteDTO.SubCasteDTO> subCasteDTOs = caste.getSubCastes().stream().map(subCaste -> {
////                	GetReligionCasteSubCasteDTO.CasteDTO.SubCasteDTO subCasteDTO = new GetReligionCasteSubCasteDTO.CasteDTO.SubCasteDTO();
////                    subCasteDTO.setSubCasteId(subCaste.getId());
////                    subCasteDTO.setSubCasteName(subCaste.getSubCasteName());
////                    return subCasteDTO;
////                }).collect(Collectors.toList());
////
////                casteDTO.setSubCastes(subCasteDTOs);
////                return casteDTO;
////            }).collect(Collectors.toList());
////
////            religionDTO.setCastes(casteDTOs);
////            return religionDTO;
////        }).collect(Collectors.toList());
////    }
////    
// 
////    public GetReligionCasteSubCasteDTO getReligionCasteSubcasteByReligionId(Long religionId, Long accountId) {
////        try {
//////            ReligionEntity religion = religionRepository.findById(religionId)
//////                    .orElseThrow(() -> new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.NOT_FOUND));
////
////        	ReligionEntity religion = religionRepository.findByIdAndAccountId(religionId, accountId)
////                    .orElseThrow(() -> new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.NOT_FOUND));
////        	
////            // Map the ReligionEntity to GetReligionCasteSubCasteDTO
////            GetReligionCasteSubCasteDTO religionDTO = new GetReligionCasteSubCasteDTO();
////            religionDTO.setReligionId(religion.getId());
////            religionDTO.setReligionName(religion.getReligionName());
////
////            List<GetReligionCasteSubCasteDTO.CasteDTO> casteDTOs = religion.getCastes().stream().map(caste -> {
////                GetReligionCasteSubCasteDTO.CasteDTO casteDTO = new GetReligionCasteSubCasteDTO.CasteDTO();
////                casteDTO.setCasteId(caste.getId());
////                casteDTO.setCasteName(caste.getCasteName());
////
////                List<GetReligionCasteSubCasteDTO.CasteDTO.SubCasteDTO> subCasteDTOs = caste.getSubCastes().stream().map(subCaste -> {
////                    GetReligionCasteSubCasteDTO.CasteDTO.SubCasteDTO subCasteDTO = new GetReligionCasteSubCasteDTO.CasteDTO.SubCasteDTO();
////                    subCasteDTO.setSubCasteId(subCaste.getId());
////                    subCasteDTO.setSubCasteName(subCaste.getSubCasteName());
////                    return subCasteDTO;
////                }).collect(Collectors.toList());
////
////                casteDTO.setSubCastes(subCasteDTOs);
////                return casteDTO;
////            }).collect(Collectors.toList());
////
////            religionDTO.setCastes(casteDTOs);
////            return religionDTO;
////
////        } catch (ThedalException e) {
////            log.error("Error retrieving religion with ID {}: {}", religionId, e.getMessage());
////            throw e;
////        } catch (Exception e) {
////            log.error("An unexpected error occurred while retrieving religion with ID {}: {}", religionId, e.getMessage());
////            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
////        }
////    }
//
//    
//    
////    @Transactional
////    public void updateReligionCasteSubcaste(ReligionCasteSubCasteUpdateDTO dto) {
////        // Update Religion
////        ReligionEntity religion = religionRepository.findById(dto.getReligionId())
////            .orElseThrow(() -> new RuntimeException("Religion not found with ID: " + dto.getReligionId()));
////        if (dto.getReligionName() != null) {
////            religion.setReligionName(dto.getReligionName());
////        }
////
////        // Update each Caste and its SubCastes
////        for (ReligionCasteSubCasteUpdateDTO.CasteUpdateDTO casteDTO : dto.getCastes()) {
////            CasteEntity caste = casteRepository.findById(casteDTO.getCasteId())
////                .orElseThrow(() -> new RuntimeException("Caste not found with ID: " + casteDTO.getCasteId()));
////            if (casteDTO.getCasteName() != null) {
////                caste.setCasteName(casteDTO.getCasteName());
////            }
////
////            // Update each SubCaste
////            for (ReligionCasteSubCasteUpdateDTO.CasteUpdateDTO.SubCasteUpdateDTO subCasteDTO : casteDTO.getSubCastes()) {
////                SubCasteEntity subCaste = subCasteRepository.findById(subCasteDTO.getSubCasteId())
////                    .orElseThrow(() -> new RuntimeException("SubCaste not found with ID: " + subCasteDTO.getSubCasteId()));
////                if (subCasteDTO.getSubCasteName() != null) {
////                    subCaste.setSubCasteName(subCasteDTO.getSubCasteName());
////                }
////            }
////        }
////
////        // Save the changes
////        religionRepository.save(religion);
////    }
//    @Transactional
//    public void updateReligionCasteSubcaste(Long religionId, ReligionCasteSubCasteUpdateDTO dto, Long accountId) {
//
//        try {
//            log.info("Updating religion with ID: {}", religionId);
//
//            //ReligionEntity religion = religionRepository.findById(religionId)
//            ReligionEntity religion = religionRepository.findByIdAndAccountId(religionId, accountId)
//                    .orElseThrow(() -> {
//                    	log.warn("Religion not found with ID: {} and Account ID: {}", religionId, accountId);
//                        return new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.NOT_FOUND);
//                    });
//
//            if (dto.getReligionName() != null) {
//                religion.setReligionName(dto.getReligionName());
//            }
//
//            for (ReligionCasteSubCasteUpdateDTO.CasteUpdateDTO casteDTO : dto.getCastes()) {
//                //CasteEntity caste = casteRepository.findById(casteDTO.getCasteId())
//            	CasteEntity caste = casteRepository.findByIdAndReligionIdAndAccountId(casteDTO.getCasteId(), religionId, accountId)
//                        .orElseThrow(() -> {
//                        	log.warn("Caste not found with ID: {} for Religion ID: {} and Account ID: {}", casteDTO.getCasteId(), religionId, accountId);
//                            return new ThedalException(ThedalError.CASTE_NOT_FOUND, HttpStatus.NOT_FOUND);
//                        });
//
//                // Update caste name if provided
//                if (casteDTO.getCasteName() != null) {
//                    caste.setCasteName(casteDTO.getCasteName());
//                }
//
//                // Iterate through sub-castes and update each sub-caste
//                for (ReligionCasteSubCasteUpdateDTO.CasteUpdateDTO.SubCasteUpdateDTO subCasteDTO : casteDTO.getSubCastes()) {
//                    //SubCasteEntity subCaste = subCasteRepository.findById(subCasteDTO.getSubCasteId())
//                	SubCasteEntity subCaste = subCasteRepository.findByIdAndCasteIdAndAccountId(subCasteDTO.getSubCasteId(), casteDTO.getCasteId(), accountId)
//                            .orElseThrow(() -> {
//                            	log.warn("SubCaste not found with ID: {} for Caste ID: {} and Account ID: {}", subCasteDTO.getSubCasteId(), caste.getId(), accountId);
//                                return new ThedalException(ThedalError.SUBCASTE_NOT_FOUND, HttpStatus.NOT_FOUND);
//                            });
//
//                    // Update sub-caste name if provided
//                    if (subCasteDTO.getSubCasteName() != null) {
//                        subCaste.setSubCasteName(subCasteDTO.getSubCasteName());
//                    }
//                }
//            }
//
//            religionRepository.save(religion);
//            log.info("Religion with ID: {} successfully updated", religionId);
//
//        } catch (ThedalException e) {
//            log.error("Error updating religion with ID: {}: {}", religionId, e.getMessage());
//            throw e; 
//        } catch (Exception e) {
//            log.error("Unexpected error updating religion with ID: {}: {}", religionId, e.getMessage());
//            throw new ThedalException(ThedalError.RELIGION_UPDATE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
//        }
//    }
//
//    
//    
//    @Transactional
//    public ThedalResponse<Void> deleteReligionCasteSubcaste(Long religionId, Long accountId) {
//    	log.info("Deleting religion with ID: {}", religionId);
//        //ReligionEntity religion = religionRepository.findById(religionId)
//        ReligionEntity religion = religionRepository.findByIdAndAccountId(religionId, accountId)
//                .orElseThrow(() -> {
//                    log.warn("Religion not found with ID: {}", religionId);
//                    return new ThedalException(ThedalError.RELIGION_NOT_FOUND, HttpStatus.NOT_FOUND);
//                });
////        religionRepository.delete(religion);
////        log.info("Religion with ID: {} successfully deleted", religionId);
//        try {
//            religionRepository.delete(religion);
//            log.info("Religion with ID: {} and associated castes/sub-castes successfully deleted for account ID: {}", religionId, accountId);
//        } catch (Exception e) {
//            log.error("Failed to delete religion with ID: {} and account ID: {}", religionId, accountId, e);
//            throw new ThedalException(ThedalError.BOOTH_DELETE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//        return new ThedalResponse<>(ThedalSuccess.RELIGION_HIERARCHY_DELETED);
//    }
//    
//}
