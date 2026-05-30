package com.thedal.thedal_app.voter;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Cipher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.dto.AadhaarDTO;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AadhaarService {

    private final RequestDetailsService requestDetails;
    private final AadhaarRepository aadhaarRepository;
    private final ElectionRepository electionRepository;
    private final RestTemplate restTemplate;
    @Autowired
    private VoterRepo voterRepository;  

    @Value("${cashfree.api.client-id}")
    private String clientId;

    @Value("${cashfree.api.client-secret}")
    private String clientSecret;

    @Value("${cashfree.api.public-key-path}")
    private String publicKeyPath;

//    @Value("${cashfree.api.otp-url:https://sandbox.cashfree.com/verification/offline-aadhaar/otp}")
//    private String otpUrl;
//
//    @Value("${cashfree.api.verify-url:https://sandbox.cashfree.com/verification/offline-aadhaar/verify}")
//    private String verifyUrl;
    @Value("${cashfree.api.otp-url:https://api.cashfree.com/verification/offline-aadhaar/otp}")
    private String otpUrl; 
    @Value("${cashfree.api.verify-url:https://api.cashfree.com/verification/offline-aadhaar/verify}")
    private String verifyUrl; 

    private PublicKey cachedPublicKey;

    @Autowired
    public AadhaarService(RequestDetailsService requestDetails, AadhaarRepository aadhaarRepository,
                          ElectionRepository electionRepository, RestTemplate restTemplate) {
        this.requestDetails = requestDetails;
        this.aadhaarRepository = aadhaarRepository;
        this.electionRepository = electionRepository;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() throws Exception {
        // Load the PEM file content
        String path = publicKeyPath.startsWith("classpath:") ? publicKeyPath.substring("classpath:".length()) : publicKeyPath;
        byte[] pemBytes = new ClassPathResource(path).getInputStream().readAllBytes();
        String pemContent = new String(pemBytes, StandardCharsets.UTF_8);

        // Remove PEM headers and footers, and decode Base64
        String base64Key = pemContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", ""); // Remove whitespace (newlines, etc.)
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);

        // Create the public key
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        cachedPublicKey = kf.generatePublic(spec);
        log.info("Public key cached successfully");
    }
    @Transactional
    public AadhaarEntity saveAadhaar(Long electionId, AadhaarDTO dto) {
        Long accountId = requestDetails.getCurrentAccountId();
        validateElectionOwnership(electionId, accountId);
        //String aadhaarNumber = (String) dto.getAadhaarData().get("aadhaarNumber");

//        if (aadhaarNumber == null || aadhaarNumber.isBlank()) {
//            throw new ThedalException(ThedalError.INVALID_AADHAAR_NUMBER, HttpStatus.BAD_REQUEST, "Aadhaar number is missing");
//        }
//
//        if (aadhaarNumber.length() != 12 || !aadhaarNumber.matches("\\d+")) {
//            throw new ThedalException(ThedalError.INVALID_AADHAAR_NUMBER, HttpStatus.BAD_REQUEST, "Aadhaar number must be exactly 12 digits");
//        }
        
        //Optional<AadhaarEntity> existing = aadhaarRepository.findByAadhaarNumberInJson(aadhaarNumber, electionId, accountId);
//        if (existing.isPresent()) {
//            throw new ThedalException(ThedalError.AADHAAR_ALREADY_EXISTS, HttpStatus.CONFLICT, "Aadhaar number is already linked to another voter");
//        }
        AadhaarEntity entity = AadhaarEntity.builder()
                .accountId(accountId)
                .electionId(electionId)
                //.aadhaarNumber(aadhaarNumber)
                .aadhaarData(dto.getAadhaarData())
                .aadhaarVerified(false)
                .build();

        return aadhaarRepository.save(entity);
    }
//    @Transactional
//    public AadhaarEntity saveAadhaar(Long electionId, AadhaarDTO dto) {
//        Long accountId = requestDetails.getCurrentAccountId();
//        validateElectionOwnership(electionId, accountId);
//
//        // Extract Aadhaar number from DTO
//        String aadhaarNumber = dto.getAadhaarNumber();
//        if (aadhaarNumber == null || aadhaarNumber.isEmpty()) {
//            throw new IllegalArgumentException("Aadhaar number is required");
//        }
//
//        // Check if Aadhaar number already exists in voters table
//        if (voterRepository.existsByAadhaarNumberAndElectionId(aadhaarNumber, electionId)) {
//            throw new IllegalArgumentException("Aadhaar number already mapped to an existing voter for this election");
//        }
//
//        // Save Aadhaar entity
//        AadhaarEntity entity = AadhaarEntity.builder()
//                .accountId(accountId)
//                .electionId(electionId)
//                .aadhaarData(dto.getAadhaarData())
//                .aadhaarVerified(false)
//                .build();
//
//        AadhaarEntity savedAadhaar = aadhaarRepository.save(entity);
//
//        // Save or update VoterEntity with Aadhaar number
//        VoterEntity voter = new VoterEntity();
//        voter.setAadhaarNumber(aadhaarNumber);
//        voter.setAadhaarVerified(false);
//        voter.setElectionId(electionId);
//        voter.setAccountId(accountId);
//    
//        voterRepository.save(voter);
//
//        return savedAadhaar;
//    }

    public AadhaarEntity getById(Long id, Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        validateElectionOwnership(electionId, accountId);

        return aadhaarRepository.findByIdAndElectionIdAndAccountId(id, electionId, accountId)
                .orElseThrow(() -> new ThedalException(ThedalError.AADHAAR_NOT_FOUND, HttpStatus.NOT_FOUND, "Aadhaar entity not found for the given ID"));
    }

    public void deleteById(Long id, Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        validateElectionOwnership(electionId, accountId);

        AadhaarEntity entity = getById(id, electionId);
        aadhaarRepository.delete(entity);
    }

    private void validateElectionOwnership(Long electionId, Long accountId) {
        Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
        if (electionOpt.isEmpty()) {
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);
        }
    }

    public Map<String, Object> requestAadhaarOtp(String aadhaarNumber) {
        if (aadhaarNumber == null || !aadhaarNumber.matches("\\d{12}")) {
            throw new ThedalException(ThedalError.INVALID_AADHAAR_NUMBER, HttpStatus.BAD_REQUEST, "Aadhaar number must be 12 digits");
        }

        try {
            String signature = generateSignature();
            log.info("Generated signature: {}", signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-version", "2023-08-01");
            headers.set("x-client-id", clientId);
            headers.set("x-client-secret", clientSecret);
            headers.set("x-cf-signature", signature);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("aadhaar_number", aadhaarNumber);

            log.info("Calling Cashfree OTP URL: {} with client-id: {}", otpUrl, clientId);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(otpUrl, HttpMethod.POST, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            log.info("Cashfree response: {}", responseBody);

            if (responseBody == null || !"SUCCESS".equals(responseBody.get("status"))) {
                throw new ThedalException(ThedalError.OTP_REQUEST_FAILED, HttpStatus.BAD_REQUEST);
            }
            return responseBody;
        } catch (HttpClientErrorException e) {
            log.error("Cashfree API error: {}", e.getResponseBodyAsString());
            throw new ThedalException(ThedalError.OTP_REQUEST_FAILED,
                    HttpStatus.BAD_REQUEST, "Cashfree error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error generating signature or making request: {}", e.getMessage());
            throw new ThedalException(ThedalError.OTP_REQUEST_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public AadhaarEntity verifyAadhaarOtp(String otp, String refId) {
        if (otp == null || otp.trim().isEmpty() || refId == null || refId.trim().isEmpty()) {
            throw new ThedalException(ThedalError.INVALID_OTP_OR_REFID, HttpStatus.BAD_REQUEST, "OTP and refId are required");
        }

        Long accountId = requestDetails.getCurrentAccountId();

        try {
            String signature = generateSignature();
            log.info("Generated signature: {}", signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-version", "2023-08-01");
            headers.set("x-client-id", clientId);
            headers.set("x-client-secret", clientSecret);
            headers.set("x-cf-signature", signature);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("otp", otp);
            requestBody.put("ref_id", refId);

            log.info("Calling Cashfree Verify URL: {} with client-id: {}", verifyUrl, clientId);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(verifyUrl, HttpMethod.POST, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null || !"VALID".equals(responseBody.get("status"))) {
                throw new ThedalException(ThedalError.OTP_VERIFICATION_FAILED, HttpStatus.BAD_REQUEST);
            }

            AadhaarEntity entity = AadhaarEntity.builder()
                    .accountId(accountId)
                    .electionId(null) // Add logic if needed
                    .aadhaarData(responseBody)
                    .aadhaarVerified(true)
                    .build();

            return aadhaarRepository.save(entity);
        } catch (HttpClientErrorException e) {
            log.error("Cashfree API error: {}", e.getResponseBodyAsString());
            throw new ThedalException(ThedalError.OTP_VERIFICATION_FAILED,
                    HttpStatus.BAD_REQUEST, "Cashfree error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error generating signature or verifying OTP: {}", e.getMessage());
            throw new ThedalException(ThedalError.OTP_VERIFICATION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private String generateSignature() throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String dataToEncrypt = clientId + "." + timestamp;
        log.info("Data to encrypt: {}", dataToEncrypt);

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, cachedPublicKey);
        byte[] encryptedBytes = cipher.doFinal(dataToEncrypt.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public List<AadhaarEntity> getAllByElectionId(Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        validateElectionOwnership(electionId, accountId);

        return aadhaarRepository.findAllByElectionIdAndAccountId(electionId, accountId);
    }

}