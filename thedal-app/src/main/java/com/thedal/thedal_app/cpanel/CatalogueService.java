package com.thedal.thedal_app.cpanel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.cpanel.dtos.CatalogueItemRequest;
import com.thedal.thedal_app.cpanel.dtos.CatalogueItemResponse;
import com.thedal.thedal_app.cpanel.dtos.CatalogueReorderRequest;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.util.RandomTokenGenerator;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CatalogueService {

    @Autowired
    private CatalogueItemRepository catalogueItemRepository;

    @Autowired
	private RequestDetailsService requestDetails;

    @Autowired
    private AwsFileUpload awsFileUpload;

    @Value("${aws.s3.banner.bucket}")
	private String s3bucket;
    @Transactional
    public ThedalResponse<CatalogueItem> createCatalogueItem(CatalogueItemRequest request) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        accountId = 0L;
        validateCatalogueItemRequest(request);

        String imageUrl;
        try {
            imageUrl = uploadImageToAWS(request.getImage());
        } catch (Exception ex) {
            throw new ThedalException(ThedalError.UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Integer maxOrderIndex = catalogueItemRepository.findMaxOrderIndexByAccountId(accountId);
        int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
        
        CatalogueItem item = new CatalogueItem();
        item.setName(request.getName());
        item.setPrice(request.getPrice());
        item.setDescription(request.getDescription());
        item.setImage(imageUrl);
        item.setAccountId(accountId);
        item.setOrderIndex(newOrderIndex);
        item.setActive(request.getActive() != null ? request.getActive() : true);

        try {
            catalogueItemRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException ex) {
            throw new ThedalException(ThedalError.DUPLICATE_CATALOGUE_ITEM_NAME, HttpStatus.CONFLICT);
        }

        return new ThedalResponse<>(ThedalSuccess.CATALOGUE_ITEM_CREATED, item);
    }

    public List<CatalogueItemResponse> getAllCatalogueItems(Long accountId) {
        accountId = 0L;
        List<CatalogueItem> items = catalogueItemRepository.findByAccountId(accountId);

        if (items.isEmpty()) {
            throw new ThedalException(ThedalError.CATALOGUE_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        return items.stream()
                .map(item -> new CatalogueItemResponse(
                        item.getId(),
                        item.getName(),
                        item.getPrice(),
                        item.getDescription(),
                        item.getImage(),
                        item.getOrderIndex(),
                        item.getActive()))
                .collect(Collectors.toList());
    }

    public CatalogueItemResponse getCatalogueItemById(Long itemId, Long accountId) {
        accountId = 0L;
        CatalogueItem item = catalogueItemRepository.findByIdAndAccountId(itemId, accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.CATALOGUE_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND));

        return new CatalogueItemResponse(
                item.getId(),
                item.getName(),
                item.getPrice(),
                item.getDescription(),
                item.getImage(),
                item.getOrderIndex(),
                item.getActive());
    }

    @Transactional
    public ThedalResponse<CatalogueItem> updateCatalogueItem(CatalogueItemRequest request) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        accountId = 0L;
        Long itemId = request.getItemId();
        if (itemId == null) {
            throw new ThedalException(ThedalError.INVALID_CATALOGUE_ITEM_ID, HttpStatus.BAD_REQUEST);
        }

        CatalogueItem item = catalogueItemRepository.findByIdAndAccountId(itemId, accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.CATALOGUE_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (request.getName() != null && !request.getName().isEmpty()) {
            item.setName(request.getName());
        }
        if (request.getPrice() != null) {
            item.setPrice(request.getPrice());
        }
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            item.setDescription(request.getDescription());
        }
        if (request.getImage() != null) {
            try {
                String newImageUrl = uploadImageToAWS(request.getImage());
                item.setImage(newImageUrl);
            } catch (Exception ex) {
                throw new ThedalException(ThedalError.UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        if (request.getActive() != null) {
            item.setActive(request.getActive()); 
        }

        catalogueItemRepository.saveAndFlush(item);
        return new ThedalResponse<>(ThedalSuccess.CATALOGUE_ITEM_UPDATED, item);
    }

    @Transactional
    public void deleteCatalogueItem(Long accountId, List<Long> itemIds) {
        try {
            int deletedCount;
            accountId = 0L;

            if (itemIds == null || itemIds.isEmpty()) {
                log.info("Deleting all catalogue items for accountId: {}", accountId);
                deletedCount = catalogueItemRepository.deleteByAccountId(accountId);
            } else {
                log.info("Deleting specific catalogue items for accountId: {}, itemIds: {}", accountId, itemIds);
                deletedCount = catalogueItemRepository.deleteByAccountIdAndIds(accountId, itemIds);
            }

            if (deletedCount == 0) {
                log.warn("No catalogue items found for accountId: {}, itemIds: {}", accountId, itemIds);
                throw new ThedalException(ThedalError.CATALOGUE_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND);
            }

            log.info("Successfully deleted {} catalogue items", deletedCount);
        } catch (ThedalException e) {
            log.error("Error while deleting catalogue items: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting catalogue items: {}", e.getMessage());
            throw new ThedalException(ThedalError.ACCESS_DENIED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void validateCatalogueItemRequest(CatalogueItemRequest request) {
        if (request.getName() == null || request.getName().isEmpty()) {
            throw new ThedalException(ThedalError.CATALOGUE_ITEM_NAME_NOT_FOUND, HttpStatus.BAD_REQUEST);
        }
        if (request.getPrice() == null || request.getPrice() < 0) {
            throw new ThedalException(ThedalError.INVALID_PRICE, HttpStatus.BAD_REQUEST);
        }
        if (request.getDescription() == null || request.getDescription().isEmpty()) {
            throw new ThedalException(ThedalError.CATALOGUE_ITEM_DESCRIPTION_NOT_FOUND, HttpStatus.BAD_REQUEST);
        }
        if (request.getImage() == null || !isValidImageFormat(request.getImage())) {
            throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
        }
        if (catalogueItemRepository.existsByNameAndAccountId(request.getName(), 0L)) {
            throw new ThedalException(ThedalError.DUPLICATE_CATALOGUE_ITEM_NAME, HttpStatus.CONFLICT);
        }
    }

    private boolean isValidImageFormat(MultipartFile image) {
        String contentType = image.getContentType();
        return contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png"));
    }

    private String uploadImageToAWS(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        if (!(MediaType.IMAGE_JPEG_VALUE.equals(contentType) || MediaType.IMAGE_PNG_VALUE.equals(contentType))) {
            throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        long maxFileSize = 5 * 1024 * 1024; // 5MB
        if (imageFile.getSize() > maxFileSize) {
            throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.BAD_REQUEST);
        }

        String fileExtension = "." + awsFileUpload.getFileExtension(imageFile.getOriginalFilename());
        String fileName = "catalogue_" + System.currentTimeMillis() + "_" + RandomTokenGenerator.generateToken(10) + fileExtension;

        try {
            File tempFile = File.createTempFile("temp", fileExtension);
            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                fileOutputStream.write(imageFile.getBytes());
            }

            String awsUrl = awsFileUpload.uploadToAWS(tempFile, fileName, s3bucket);

            if (!tempFile.delete()) {
                log.warn("Temporary file deletion failed: {}", tempFile.getName());
            }

            return awsUrl;
        } catch (IOException e) {
            log.error("Error uploading image to AWS S3", e);
            throw new ThedalException(ThedalError.INVALID_IMAGE_FORMAT, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    @Transactional
    public ThedalResponse<String> updateCatalogueItemOrder(List<CatalogueReorderRequest> reorderRequests, Long accountId) {
        // Fetch all catalogue items for the given account, sorted by orderIndex
        List<CatalogueItem> items = catalogueItemRepository.findByAccountIdOrderByOrderIndexAsc(accountId);

        if (items.isEmpty()) {
            log.error("No catalogue items found for account ID {}", accountId);
            throw new ThedalException(ThedalError.CATALOGUE_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        // Create a map of itemId -> newOrderIndex
        Map<Long, Integer> newOrderMap = reorderRequests.stream()
                .collect(Collectors.toMap(CatalogueReorderRequest::getItemId, CatalogueReorderRequest::getNewOrderIndex));

        // Sort the reorderRequests by newOrderIndex to avoid conflicts
        reorderRequests.sort(Comparator.comparingInt(CatalogueReorderRequest::getNewOrderIndex));

        // Collect items that are being reordered
        List<CatalogueItem> reorderedItems = new ArrayList<>();
        List<CatalogueItem> remainingItems = new ArrayList<>(items);

        for (CatalogueReorderRequest request : reorderRequests) {
            CatalogueItem item = items.stream()
                    .filter(i -> i.getId().equals(request.getItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ThedalException(ThedalError.CATALOGUE_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND));

            reorderedItems.add(item);
            remainingItems.remove(item);
        }

        // Insert reordered items at their new positions
        for (CatalogueReorderRequest request : reorderRequests) {
            CatalogueItem item = reorderedItems.stream()
                    .filter(i -> i.getId().equals(request.getItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ThedalException(ThedalError.CATALOGUE_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND));

            // Ensure the new index is within bounds
            int newIndex = Math.min(request.getNewOrderIndex(), remainingItems.size());
            remainingItems.add(newIndex, item);
        }

        // Update orderIndex for all items
        for (int i = 0; i < remainingItems.size(); i++) {
            remainingItems.get(i).setOrderIndex(i);
            log.info("Updated catalogue item order: {} -> {}", remainingItems.get(i).getName(), i);
        }

        // Save updated order to DB
        catalogueItemRepository.saveAll(remainingItems);
        log.info("Catalogue item order updated successfully for accountId: {}", accountId);

        return new ThedalResponse<>(ThedalSuccess.CATALOGUE_ITEM_ORDER_UPDATED);
    }
    
    public List<CatalogueItemResponse> getActiveCatalogueItems(Long accountId) {
        accountId = 0L;
        List<CatalogueItem> items = catalogueItemRepository.findByAccountIdAndActiveTrue(accountId);

        if (items.isEmpty()) {
            throw new ThedalException(ThedalError.CATALOGUE_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        return items.stream()
                .map(item -> new CatalogueItemResponse(
                        item.getId(),
                        item.getName(),
                        item.getPrice(),
                        item.getDescription(),
                        item.getImage(),
                        item.getOrderIndex(),
                        item.getActive()))
                .collect(Collectors.toList());
    }
    
    
}