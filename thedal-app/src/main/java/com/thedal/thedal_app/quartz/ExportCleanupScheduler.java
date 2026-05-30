package com.thedal.thedal_app.quartz;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.election.SurveyDownloadJob;
import com.thedal.thedal_app.election.SurveyDownloadJobRepository;
import com.thedal.thedal_app.voter.VoterDownloadJob;
import com.thedal.thedal_app.voter.VoterDownloadJobRepository;

@Component
public class ExportCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExportCleanupScheduler.class);

    @Autowired
    private VoterDownloadJobRepository voterDownloadJobRepository;

    @Autowired
    private SurveyDownloadJobRepository surveyDownloadJobRepository;

    @Autowired
    private AwsFileUpload awsFileUpload;

    @Value("${aws.s3.files.bucket}")
    private String s3Bucket;

    @Scheduled(cron = "0 0 * * * ?") // Run every hour
    @Transactional
    public void cleanupExpiredJobs() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<VoterDownloadJob> voterJobs = voterDownloadJobRepository.findByStatusAndTimeCompletedBefore("COMPLETED", threshold);
        for (VoterDownloadJob job : voterJobs) {
            String fileKey = "voter_exports/voter_export_" + job.getId() + ".xlsx";
            try {
                deleteS3FileWithRetry(fileKey, job.getId(), "voter");
                voterDownloadJobRepository.delete(job);
                log.info("Fallback cleanup: Deleted voter jobId: {}", job.getId());
            } catch (Exception e) {
                log.error("Failed to cleanup voter jobId: {}: {}", job.getId(), e.getMessage());
            }
        }

        List<SurveyDownloadJob> surveyJobs = surveyDownloadJobRepository.findByStatusAndTimeCompletedBefore("COMPLETED", threshold);
        for (SurveyDownloadJob job : surveyJobs) {
            String fileKey = "survey_exports/survey_export_" + job.getId() + ".xlsx";
            try {
                deleteS3FileWithRetry(fileKey, job.getId(), "survey");
                surveyDownloadJobRepository.delete(job);
                log.info("Fallback cleanup: Deleted survey jobId: {}", job.getId());
            } catch (Exception e) {
                log.error("Failed to cleanup survey jobId: {}: {}", job.getId(), e.getMessage());
            }
        }
    }

    private void deleteS3FileWithRetry(String fileKey, Long jobId, String type) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                awsFileUpload.deleteS3Object(s3Bucket, fileKey);
                log.info("Deleted S3 file for {} jobId: {}", type, jobId);
                return;
            } catch (Exception e) {
                log.warn("Attempt {} failed to delete S3 file for {} jobId: {}: {}", 
                         attempt, type, jobId, e.getMessage());
                if (attempt == maxRetries) {
                    log.error("Failed to delete S3 file after {} attempts for {} jobId: {}", 
                              maxRetries, type, jobId);
                    throw new RuntimeException("Failed to delete S3 file: " + fileKey, e);
                }
                try {
                    Thread.sleep(1000 * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}