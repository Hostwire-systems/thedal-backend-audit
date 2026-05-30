package com.thedal.thedal_app.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.thedal.thedal_app.util.ApiService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SmsNotification {
    private final ApiService apiService;
    
    @Value("${msg91.authkey}")
    private String authKey;

    @Value("${msg91.template_id}")
    private String templateId;

   // public static final String MSG91_URL = "https://control.msg91.com/api/v5/otp?realTimeResponse=1";

    public SmsNotification(ApiService apiService) {
        this.apiService = apiService;
    }

    

   public boolean sendTransactionalOTP(String mobileNumber, String otp) {
    try {
        // Build URL safely
        String apiUrl = UriComponentsBuilder.fromHttpUrl("https://control.msg91.com/api/v5/otp")
                .queryParam("otp_expiry", "")
                .queryParam("template_id", templateId)
                .queryParam("mobile", "91" + mobileNumber)
                .queryParam("authkey", authKey)
                .queryParam("realTimeResponse", "1")
                .toUriString();

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Cookie", "HELLO_APP_HASH=V3lCYzgxY...; PHPSESSID=riqg185hhet53ld437cfltipo0"); // If required

        // Set request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("OTP", otp);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Make the POST request
        ResponseEntity<String> response = apiService.makePostRequestWithParams(apiUrl, entity);

        log.info("POST Response: {} | Status Code: {}", response.getBody(), response.getStatusCode());

        return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
        log.error("Error sending OTP", e);
        return false;
    }
}

    /**
     * Send bulk SMS to multiple recipients
     * @param mobileNumbers List of mobile numbers (without country code)
     * @param message SMS content (plain text only)
     * @param senderName SMS sender name (6 chars alphanumeric)
     * @return BulkSmsResult containing success/failure counts
     */
    @Async
    public CompletableFuture<BulkSmsResult> sendBulkSms(List<String> mobileNumbers, String message, String senderName) {
        if (mobileNumbers == null || mobileNumbers.isEmpty()) {
            return CompletableFuture.completedFuture(new BulkSmsResult(0, 0, "No recipients provided"));
        }

        // Validate message length (160 chars for single SMS)
        if (message.length() > 160) {
            log.warn("SMS message exceeds 160 characters, may be charged as multiple SMS");
        }

        int batchSize = 100; // Process in batches to avoid overwhelming the API
        int totalSent = 0;
        int totalFailed = 0;

        try {
            // Process in batches
            for (int i = 0; i < mobileNumbers.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, mobileNumbers.size());
                List<String> batch = mobileNumbers.subList(i, endIndex);
                
                log.info("Processing SMS batch {}/{}: {} recipients", 
                    (i / batchSize) + 1, 
                    (mobileNumbers.size() + batchSize - 1) / batchSize,
                    batch.size());

                for (String mobileNumber : batch) {
                    try {
                        boolean sent = sendSingleSms(mobileNumber, message, senderName);
                        if (sent) {
                            totalSent++;
                        } else {
                            totalFailed++;
                        }
                        
                        // Small delay between messages to avoid rate limiting
                        Thread.sleep(100);
                    } catch (Exception e) {
                        log.error("Failed to send SMS to {}: {}", mobileNumber, e.getMessage());
                        totalFailed++;
                    }
                }
                
                // Delay between batches
                if (endIndex < mobileNumbers.size()) {
                    Thread.sleep(2000);
                }
            }

            log.info("Bulk SMS completed. Sent: {}, Failed: {}", totalSent, totalFailed);
            return CompletableFuture.completedFuture(
                new BulkSmsResult(totalSent, totalFailed, "Bulk SMS sending completed"));

        } catch (Exception e) {
            log.error("Error in bulk SMS sending: {}", e.getMessage());
            return CompletableFuture.completedFuture(
                new BulkSmsResult(totalSent, totalFailed, "Error: " + e.getMessage()));
        }
    }

    /**
     * Send single SMS (promotional/transactional)
     */
    private boolean sendSingleSms(String mobileNumber, String message, String senderName) {
        try {
            // Use MSG91 promotional SMS API for bulk campaigns
            String apiUrl = UriComponentsBuilder.fromHttpUrl("https://control.msg91.com/api/sendhttp.php")
                    .queryParam("authkey", authKey)
                    .queryParam("mobiles", "91" + mobileNumber)
                    .queryParam("message", message)
                    .queryParam("sender", senderName != null ? senderName : "THEDAL")
                    .queryParam("route", "4") // Promotional route
                    .queryParam("country", "91")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            // Use simple GET request for promotional SMS
            String response = apiService.getRequest(apiUrl);
            
            // MSG91 returns success response with message ID, error responses contain "ERROR"
            return response != null && !response.contains("ERROR");

        } catch (Exception e) {
            log.error("Error sending SMS to {}: {}", mobileNumber, e.getMessage());
            return false;
        }
    }

    /**
     * Validate and clean SMS content for SMS channel
     */
    public String prepareSmsContent(String htmlContent) {
        if (htmlContent == null) return "";
        
        // Strip HTML tags and decode entities
        String plainText = htmlContent.replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .trim();

        // Truncate if too long (keeping some buffer)
        if (plainText.length() > 155) {
            plainText = plainText.substring(0, 155) + "...";
        }

        return plainText;
    }

    /**
     * Result class for bulk SMS operations
     */
    public static class BulkSmsResult {
        private final int successCount;
        private final int failureCount;
        private final String message;

        public BulkSmsResult(int successCount, int failureCount, String message) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.message = message;
        }

        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public String getMessage() { return message; }
        public int getTotalCount() { return successCount + failureCount; }
        public boolean isSuccess() { return failureCount == 0; }
    }

}
