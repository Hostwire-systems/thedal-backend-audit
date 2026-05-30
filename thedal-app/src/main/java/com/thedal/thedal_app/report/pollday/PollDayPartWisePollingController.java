package com.thedal.thedal_app.report.pollday;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.report.aggregates.RecomputeRateLimiter;
import com.thedal.thedal_app.report.pollday.dto.PartWisePollingResponse;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reporting/poll-day/part-wise-polling")
@RequiredArgsConstructor
public class PollDayPartWisePollingController {

    private final PollDayPartWisePollingService service;
    private final RecomputeRateLimiter rateLimiter;
    private final RequestDetailsService requestDetails;

    @GetMapping
    public ResponseEntity<PartWisePollingResponse> get(
            @RequestParam Long electionId,
            @RequestParam(required = false) String pollingDate,
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
        
        // Parse comma-separated filter parameters
        List<String> partyList = parseCommaSeparated(parties);
        List<String> religionList = parseCommaSeparated(religions);
        List<String> casteCategoryList = parseCommaSeparated(casteCategories);
        List<String> casteList = parseCommaSeparated(castes);
        List<String> subCasteList = parseCommaSeparated(subCastes);
        List<String> languageList = parseCommaSeparated(languages);
        List<String> schemeList = parseCommaSeparated(schemes);
        List<String> genderList = parseCommaSeparated(genders);
        
        PartWisePollingResponse data = service.getPartWisePolling(
            accountId, electionId, date,
            partyList, religionList, casteCategoryList, casteList, subCasteList,
            languageList, schemeList, genderList, minAge, maxAge, includeUnknownAge
        );

        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=60")
                .body(data);
    }

    @PostMapping("/recompute")
    public ResponseEntity<PartWisePollingResponse> recompute(
            @RequestParam Long electionId,
            @RequestParam(required = false) String pollingDate,
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
        
        if (!rateLimiter.allow("poll-day-part-wise-polling", accountId, electionId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        LocalDate date = pollingDate != null ? LocalDate.parse(pollingDate) : null;
        
        // Parse comma-separated filter parameters
        List<String> partyList = parseCommaSeparated(parties);
        List<String> religionList = parseCommaSeparated(religions);
        List<String> casteCategoryList = parseCommaSeparated(casteCategories);
        List<String> casteList = parseCommaSeparated(castes);
        List<String> subCasteList = parseCommaSeparated(subCastes);
        List<String> languageList = parseCommaSeparated(languages);
        List<String> schemeList = parseCommaSeparated(schemes);
        List<String> genderList = parseCommaSeparated(genders);
        
        PartWisePollingResponse data = service.recomputePartWisePolling(
            accountId, electionId, date,
            partyList, religionList, casteCategoryList, casteList, subCasteList,
            languageList, schemeList, genderList, minAge, maxAge, includeUnknownAge
        );
        
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=60")
                .body(data);
    }
    
    /**
     * Parse comma-separated string into list
     * Returns null if input is null or empty
     */
    private List<String> parseCommaSeparated(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        return java.util.Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }
}
