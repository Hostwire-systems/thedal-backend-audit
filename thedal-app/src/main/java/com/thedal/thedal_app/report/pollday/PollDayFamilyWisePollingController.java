package com.thedal.thedal_app.report.pollday;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.report.aggregates.RecomputeRateLimiter;
import com.thedal.thedal_app.report.pollday.dto.FamilyWisePollingResponse;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reporting/poll-day/family-wise-polling")
@RequiredArgsConstructor
public class PollDayFamilyWisePollingController {

    private final PollDayFamilyWisePollingService service;
    private final RecomputeRateLimiter rateLimiter;
    private final RequestDetailsService requestDetails;

    @GetMapping
    public ResponseEntity<FamilyWisePollingResponse> get(
            @RequestParam Long electionId,
            @RequestParam(required = false) String pollingDate,
            @RequestParam(required = false) String partNumbers,
            @RequestParam(required = false) String parties,
            @RequestParam(required = false) String religions,
            @RequestParam(required = false) String casteCategories,
            @RequestParam(required = false) String castes,
            @RequestParam(required = false) String subCastes,
            @RequestParam(required = false) String languages,
            @RequestParam(required = false) String schemes,
            @RequestParam(required = false) String genders,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) Boolean includeUnknownAge) {
        
        // Get account ID from JWT token
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        LocalDate date = pollingDate != null ? LocalDate.parse(pollingDate) : null;
        List<Integer> partNumberList = parsePartNumbers(partNumbers);
        
        // Parse filter parameters
        List<String> partyList = parseCommaSeparated(parties);
        List<String> religionList = parseCommaSeparated(religions);
        List<String> casteCategoryList = parseCommaSeparated(casteCategories);
        List<String> casteList = parseCommaSeparated(castes);
        List<String> subCasteList = parseCommaSeparated(subCastes);
        List<String> languageList = parseCommaSeparated(languages);
        List<String> schemeList = parseCommaSeparated(schemes);
        List<String> genderList = parseCommaSeparated(genders);
        
        FamilyWisePollingResponse data = service.getFamilyWisePolling(
            accountId, electionId, date, partNumberList,
            partyList, religionList, casteCategoryList, casteList, subCasteList,
            languageList, schemeList, genderList, minAge, maxAge, includeUnknownAge
        );

        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=60")
                .body(data);
    }

    @PostMapping("/recompute")
    public ResponseEntity<FamilyWisePollingResponse> recompute(
            @RequestParam Long electionId,
            @RequestParam(required = false) String pollingDate,
            @RequestParam(required = false) String partNumbers,
            @RequestParam(required = false) String parties,
            @RequestParam(required = false) String religions,
            @RequestParam(required = false) String casteCategories,
            @RequestParam(required = false) String castes,
            @RequestParam(required = false) String subCastes,
            @RequestParam(required = false) String languages,
            @RequestParam(required = false) String schemes,
            @RequestParam(required = false) String genders,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) Boolean includeUnknownAge) {
        
        // Get account ID from JWT token
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        if (!rateLimiter.allow("poll-day-family-wise-polling", accountId, electionId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        LocalDate date = pollingDate != null ? LocalDate.parse(pollingDate) : null;
        List<Integer> partNumberList = parsePartNumbers(partNumbers);
        
        // Parse filter parameters
        List<String> partyList = parseCommaSeparated(parties);
        List<String> religionList = parseCommaSeparated(religions);
        List<String> casteCategoryList = parseCommaSeparated(casteCategories);
        List<String> casteList = parseCommaSeparated(castes);
        List<String> subCasteList = parseCommaSeparated(subCastes);
        List<String> languageList = parseCommaSeparated(languages);
        List<String> schemeList = parseCommaSeparated(schemes);
        List<String> genderList = parseCommaSeparated(genders);
        
        FamilyWisePollingResponse data = service.recomputeFamilyWisePolling(
            accountId, electionId, date, partNumberList,
            partyList, religionList, casteCategoryList, casteList, subCasteList,
            languageList, schemeList, genderList, minAge, maxAge, includeUnknownAge
        );
        
        return ResponseEntity.ok(data);
    }

    @DeleteMapping("/cache")
    public ResponseEntity<String> clearCache(
            @RequestParam Long electionId,
            @RequestParam(required = false) String pollingDate) {
        
        // Get account ID from JWT token
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        LocalDate date = pollingDate != null ? LocalDate.parse(pollingDate) : null;
        service.clearCache(accountId, electionId, date);
        
        return ResponseEntity.ok("Cache cleared successfully. Next request will recompute with fresh data.");
    }

    /**
     * Parse comma-separated part numbers string into list of integers
     * Returns null if input is null or empty (meaning all parts)
     */
    private List<Integer> parsePartNumbers(String partNumbers) {
        if (partNumbers == null || partNumbers.trim().isEmpty()) {
            return null; // null means all parts
        }
        
        return Arrays.stream(partNumbers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
    
    /**
     * Parse comma-separated string into list
     * Returns null if input is null or empty
     */
    private List<String> parseCommaSeparated(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
