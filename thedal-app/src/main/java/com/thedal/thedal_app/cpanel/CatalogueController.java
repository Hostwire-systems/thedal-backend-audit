package com.thedal.thedal_app.cpanel;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.cpanel.dtos.CatalogueItemRequest;
import com.thedal.thedal_app.cpanel.dtos.CatalogueItemResponse;
import com.thedal.thedal_app.cpanel.dtos.CatalogueReorderRequest;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "Catalogue Controller")
public class CatalogueController {

    @Autowired
    private CatalogueService catalogueService;

    @Autowired
	private RequestDetailsService requestDetails;

    @Operation(summary = "Create a catalogue item", description = "Creates a new catalogue item with the provided details", 
               tags = { "Catalogue Controller" })
    @PostMapping(value = "/cpanel/catalogue", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ThedalResponse<CatalogueItem> createCatalogueItem(
            @RequestPart("image") MultipartFile image,
            @RequestParam("name") String name,
            @RequestParam("price") Double price,
            @RequestParam("description") String description,
            @RequestParam(value = "active", required = false, defaultValue = "true") Boolean active) {

        CatalogueItemRequest request = new CatalogueItemRequest();
        request.setName(name);
        request.setPrice(price);
        request.setDescription(description);
        request.setImage(image);
        request.setActive(active);

        return catalogueService.createCatalogueItem(request);
    }

    @Operation(summary = "Get all catalogue items", description = "Retrieve all catalogue items", 
               tags = { "Catalogue Controller" })
    @GetMapping("/cpanel/catalogue")
    public ThedalResponse<List<CatalogueItemResponse>> getAllCatalogueItems() {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        accountId = 0L;
        List<CatalogueItemResponse> items = catalogueService.getAllCatalogueItems(accountId);
        return new ThedalResponse<>(ThedalSuccess.CATALOGUE_ITEMS_FETCHED, items);
    }

    @Operation(summary = "Get a catalogue item by ID", description = "Retrieve a specific catalogue item by its ID", 
               tags = { "Catalogue Controller" })
    @GetMapping("/cpanel/catalogue/{itemId}")
    public ThedalResponse<CatalogueItemResponse> getCatalogueItemById(@PathVariable Long itemId) {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        accountId = 0L;
        CatalogueItemResponse item = catalogueService.getCatalogueItemById(itemId, accountId);
        return new ThedalResponse<>(ThedalSuccess.CATALOGUE_ITEM_FETCHED, item);
    }

    @Operation(summary = "Update a catalogue item", description = "Updates an existing catalogue item with the provided details", 
               tags = { "Catalogue Controller" })
    @PutMapping(value = "/cpanel/catalogue", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ThedalResponse<CatalogueItem> updateCatalogueItem(
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestParam("itemId") Long itemId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "price", required = false) Double price,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "active", required = false) Boolean active) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        accountId = 0L;

        CatalogueItemRequest request = new CatalogueItemRequest();
        request.setItemId(itemId);
        request.setName(name);
        request.setPrice(price);
        request.setDescription(description);
        request.setImage(image);
        request.setActive(active);

        return catalogueService.updateCatalogueItem(request);
    }

    @Operation(summary = "Delete a catalogue item", description = "Deletes a catalogue item by ID", 
               tags = { "Catalogue Controller" })
    @DeleteMapping("/cpanel/catalogue")
    public ThedalResponse<Void> deleteCatalogueItem(@RequestParam(value = "itemIds", required = false) List<Long> itemIds) {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        accountId = 0L;
        catalogueService.deleteCatalogueItem(accountId, itemIds);
        return new ThedalResponse<>(ThedalSuccess.CATALOGUE_ITEM_DELETED, null);
    }
    
    @Operation(summary = "Reorder catalogue items", description = "Reorders catalogue items for an account")
    @PutMapping("/cpanel/catalogue/reorder")
    public ResponseEntity<ThedalResponse<String>> reorderCatalogueItems(
            @RequestBody List<CatalogueReorderRequest> reorderRequests) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            // Temporary override for testing (remove in production)
            accountId = 0L;

            ThedalResponse<String> response = catalogueService.updateCatalogueItemOrder(reorderRequests, accountId);
            return ResponseEntity.ok(response);

        } catch (ThedalException e) {
            log.error("Error reordering catalogue items: {}", e.getMessage());
            return ResponseEntity.status(e.getHttpStatus())
                    .body(new ThedalResponse<>(e.getThedalError()));
        } catch (Exception e) {
            log.error("Unexpected error reordering catalogue items: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThedalResponse<>(ThedalError.CATALOGUE_ITEM_ORDER_UPDATE_FAILED));
        }
    }
    
    @Operation(summary = "Get all active catalogue items for app", description = "Retrieve all active catalogue items for the app", 
            tags = { "Catalogue Controller" })
 @GetMapping("/app/catalogue")
 public ThedalResponse<List<CatalogueItemResponse>> getActiveCatalogueItems() {
     Long accountId = requestDetails.getCurrentAccountId();

     if (accountId == null) {
         log.error("Account ID not found, unauthorized access.");
         throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
     }

     accountId = 0L;
     List<CatalogueItemResponse> items = catalogueService.getActiveCatalogueItems(accountId);
     return new ThedalResponse<>(ThedalSuccess.CATALOGUE_ITEMS_FETCHED, items);
 }
    
}