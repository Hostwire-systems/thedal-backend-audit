package com.thedal.thedal_app.voter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.dto.VoterResponseMongoDTO;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/voters")
@Slf4j
public class VoterMongoController {
	
	@Autowired
    private VoterMongoService voterMongoService;
    
    @Autowired
    private RequestDetailsService requestDetails;
    
    private static final Set<String> PART_NO_VARIATIONS = Set.of("part_number", "partno", "partnumber");
    private static final Set<String> SERIAL_NO_VARIATIONS = Set.of("serial_number", "serialno", "serialnumber");
    private static final Set<String> FNAME_VARIATIONS = Set.of("voter_fname_en", "voterfnameen", "firstname");
    private static final Set<String> LNAME_VARIATIONS = Set.of("voter_lname_en", "voterlnameen", "lastname");
    private static final Set<String> FULL_ADDRESS_VARIATIONS = Set.of("full_address", "fulladdress");
    private static final Set<String> AGE_VARIATIONS = Set.of("age");

    @Operation(summary = "Retrieve a list of voters from MongoDB (Dedicated MongoDB Endpoint)",
            description = "Fetches voters directly from MongoDB with all filtering capabilities. This is a dedicated MongoDB endpoint that provides the same functionality as the PostgreSQL endpoint but reads exclusively from MongoDB for better performance. Supports all query parameters including voter-id, epic-number, booth-number, family-id, names, party, religion, age, gender, etc. Includes gender statistics and sorting.",
            tags = {"Voter Management - MongoDB"})
    @GetMapping("/election/{electionId}")
    public ThedalResponse<VoterResponseMongoDTO> getVotersFromMongo(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "voter-id", required = false) String voterId,
            @RequestParam(value = "epic-number", required = false) String epicNumber,
            @RequestParam(value = "booth-number", required = false) String boothNumbers,
            @RequestParam(value = "family-id", required = false) UUID familyId,
            @RequestParam(value = "voterFnameEn", required = false) String voterFnameEn,
            @RequestParam(value = "voterLnameEn", required = false) String voterLnameEn,
            @RequestParam(value = "voterFnameL1", required = false) String voterFnameL1,
            @RequestParam(value = "voterFnameL2", required = false) String voterFnameL2,
            @RequestParam(value = "relationFirstNameEn", required = false) String relationFirstNameEn,
            @RequestParam(value = "relationLastNameEn", required = false) String relationLastNameEn,
            @RequestParam(value = "party", required = false) String partyName,
            @RequestParam(value = "religion", required = false) String religionName,
            @RequestParam(value = "voterHistoryName", required = false) String voterHistoryName,
            @RequestParam(value = "age", required = false) Integer age,
            @RequestParam(value = "minAge", required = false) Integer minAge,
            @RequestParam(value = "maxAge", required = false) Integer maxAge,
            @RequestParam(value = "includeUnknownAge", defaultValue = "false") Boolean includeUnknownAge,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "hasDob", required = false) String hasDob,
            @RequestParam(value = "starNumber", required = false) Boolean starNumber,
            @RequestParam(value = "catagoryDescription", required = false) String description,
            @RequestParam(value = "duplicate", required = false) String duplicate,
            @RequestParam(value = "serial-no", required = false) Long serialNo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "part_number,serial_number") String sortBy,
            @RequestParam(value = "order", defaultValue = "asc") String order) {

        // Force page size to 10 for stability
        size = 10;

        log.info("MongoDB GET Request params: electionId={}, voterId={}, epicNumber={}, boothNumbers={}, familyId={}, voterFnameEn={}, voterLnameEn={}, voterFnameL1={}, voterFnameL2={}, relationFirstNameEn={}, relationLastNameEn={}, partyName={}, religionName={}, voterHistoryName={}, age={}, minAge={}, maxAge={}, includeUnknownAge={}, gender={}, hasDob={}, starNumber={}, description={}, page={}, size={}, sortBy={}, order={}",
                electionId, voterId, epicNumber, boothNumbers, familyId, voterFnameEn, voterLnameEn, voterFnameL1, voterFnameL2, relationFirstNameEn, relationLastNameEn, partyName, religionName, voterHistoryName, age, minAge, maxAge, includeUnknownAge, gender, hasDob, starNumber, description, page, size, sortBy, order);

        // Validate account access
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Sort validation
        List<String> sortFields = Arrays.stream(sortBy.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        List<String> mappedSortFields = new ArrayList<>();
        for (String sortField : sortFields) {
            if (PART_NO_VARIATIONS.contains(sortField)) {
                mappedSortFields.add("partNo");
            } else if (SERIAL_NO_VARIATIONS.contains(sortField)) {
                mappedSortFields.add("serialNo");
            } else if (FNAME_VARIATIONS.contains(sortField)) {
                mappedSortFields.add("voterFnameEn");
            } else if (LNAME_VARIATIONS.contains(sortField)) {
                mappedSortFields.add("voterLnameEn");
            } else if (FULL_ADDRESS_VARIATIONS.contains(sortField)) {
                mappedSortFields.add("fullAddress");
            } else if (AGE_VARIATIONS.contains(sortField)) {
                mappedSortFields.add("age");
            } else {
                log.error("Invalid sortBy parameter: {}. Must be one of: part_number, partNo, partNumber, serial_number, serialNo, serialNumber, voter_fname_en, voter_lname_en, full_address, age (case-insensitive)", sortField);
                throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
            }
        }

        String orderLower = order.toLowerCase();
        if (!orderLower.equals("asc") && !orderLower.equals("desc")) {
            log.error("Invalid order parameter: {}. Must be 'asc' or 'desc' (case-insensitive)", order);
            throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
        }

        // Validate and convert hasDob parameter
        Boolean dobFilter = null;
        if (hasDob != null) {
            String hasDobLower = hasDob.toLowerCase();
            if ("yes".equals(hasDobLower)) {
                dobFilter = true;
            } else if ("no".equals(hasDobLower)) {
                dobFilter = false;
            } else {
                log.error("Invalid hasDob parameter: {}. Must be 'yes' or 'no' (case-insensitive)", hasDob);
                throw new ThedalException(ThedalError.INVALID_DOB, HttpStatus.BAD_REQUEST);
            }
        }

        // Parse and validate gender parameter
        List<String> genderList = null;
        if (gender != null && !gender.isEmpty()) {
            genderList = Arrays.stream(gender.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            Set<String> validGenders = Set.of("male", "female", "other");
            List<String> invalidGenders = genderList.stream()
                    .filter(g -> !validGenders.contains(g))
                    .collect(Collectors.toList());

            if (!invalidGenders.isEmpty()) {
                log.error("Invalid gender values: {}. Must be one or more of: male, female, other (case-insensitive)", String.join(", ", invalidGenders));
                throw new ThedalException(ThedalError.INVALID_GENDER, HttpStatus.BAD_REQUEST);
            }
        }

        // Parse input parameters
        List<String> voterFnameEnList = parseList(voterFnameEn);
        List<String> voterLnameEnList = parseList(voterLnameEn);
        List<String> voterFnameL1List = parseList(voterFnameL1);
        List<String> voterFnameL2List = parseList(voterFnameL2);
        List<String> relationFirstNameEnList = parseList(relationFirstNameEn);
        List<String> relationLastNameEnList = parseList(relationLastNameEn);
        List<String> partyNameList = parseList(partyName);
        List<String> voterHistoryNameList = parseList(voterHistoryName);
        List<Integer> boothNumberList = parseBoothNumbers(boothNumbers);

        // Create Pageable with proper sorting
        Sort.Direction direction = orderLower.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, mappedSortFields.toArray(new String[0]));
        Pageable pageable = PageRequest.of(page, size, sort);

        // Construct sort Document for MongoDB
        Document sortDocument = new Document();
        int sortDirection = order.equalsIgnoreCase("desc") ? -1 : 1;
        for (String field : mappedSortFields) {
            sortDocument.append(field, sortDirection);
        }
        if (sortDocument.isEmpty()) {
            sortDocument.append("partNo", 1).append("serialNo", 1);
        }
        Boolean findDuplicates = "yes".equalsIgnoreCase(duplicate);

        // Call service
        VoterResponseMongoDTO response = voterMongoService.getVoters(
                accountId, voterId, epicNumber, electionId,
                boothNumberList, familyId, voterFnameEnList,
                voterLnameEnList, voterFnameL1List, voterFnameL2List,
                relationFirstNameEnList, relationLastNameEnList,
                voterHistoryNameList, partyNameList, religionName,
                age, minAge, maxAge, includeUnknownAge, genderList,
                dobFilter, starNumber, description, null, findDuplicates, serialNo, pageable, sortDocument);

        return new ThedalResponse<>(ThedalSuccess.VOTER_FOUND, response);
    }

    @Operation(summary = "Search voters by name in MongoDB",
            description = "Searches for voters by first or last name with fuzzy matching, reading directly from MongoDB for better performance. Returns voter details and gender statistics.",
            tags = {"Voter Management - MongoDB"})
    @GetMapping("/election/{electionId}/search")
    public ThedalResponse<VoterResponseMongoDTO> searchVotersInMongo(
            @PathVariable("electionId") Long electionId,
            @RequestParam("query") String searchQuery,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "order", defaultValue = "asc") String order) {

        // Force page size to 10 for stability
        size = 10;

        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            log.error("Search query cannot be empty");
            throw new ThedalException(ThedalError.INVALID_SEARCH_QUERY, HttpStatus.BAD_REQUEST);
        }

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Determine sort fields: default partNo asc then serialNo asc for determinism
        List<String> mappedSortFields = new ArrayList<>();
    if (sortBy != null && !sortBy.trim().isEmpty()) {
            List<String> fieldTokens = Arrays.stream(sortBy.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
            for (String token : fieldTokens) {
        // Use local variation sets instead of accessing private constants from VoterController
        if (PART_NO_VARIATIONS.contains(token)) {
                    if (!mappedSortFields.contains("partNo")) mappedSortFields.add("partNo");
        } else if (SERIAL_NO_VARIATIONS.contains(token)) {
                    if (!mappedSortFields.contains("serialNo")) mappedSortFields.add("serialNo");
        } else if (FNAME_VARIATIONS.contains(token)) {
                    if (!mappedSortFields.contains("voterFnameEn")) mappedSortFields.add("voterFnameEn");
        } else if (LNAME_VARIATIONS.contains(token)) {
                    if (!mappedSortFields.contains("voterLnameEn")) mappedSortFields.add("voterLnameEn");
                } else {
                    log.error("Invalid sortBy parameter in Mongo search: {}", token);
                    throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
                }
            }
        }
        if (mappedSortFields.isEmpty()) {
            mappedSortFields.add("partNo");
            mappedSortFields.add("serialNo");
        }
        String orderLower = order == null ? "asc" : order.toLowerCase();
        if (!orderLower.equals("asc") && !orderLower.equals("desc")) {
            log.error("Invalid order parameter: {}. Must be 'asc' or 'desc'", order);
            throw new ThedalException(ThedalError.INVALID_SORT_FIELD, HttpStatus.BAD_REQUEST);
        }
        Sort.Direction direction = orderLower.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, mappedSortFields.get(0));
        for (int i = 1; i < mappedSortFields.size(); i++) {
            sort = sort.and(Sort.by(direction, mappedSortFields.get(i)));
        }
        Pageable pageable = PageRequest.of(page, size, sort);
        Document sortDocument = new Document();
        int dir = direction == Sort.Direction.ASC ? 1 : -1;
        mappedSortFields.forEach(f -> sortDocument.append(f, dir));

        VoterResponseMongoDTO result = voterMongoService.searchVotersByName(accountId, electionId, searchQuery, pageable, sortDocument);

        return new ThedalResponse<>(ThedalSuccess.VOTER_FOUND, result);
    }

    // Helper methods
    private List<String> parseList(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase) // Normalize to lowercase
                .collect(Collectors.toList());
    }

    private List<Integer> parseBoothNumbers(String boothNumbers) {
        if (boothNumbers == null || boothNumbers.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return Arrays.stream(boothNumbers.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.warn("Invalid booth number format: {}", boothNumbers);
            return Collections.emptyList();
        }
    }

}