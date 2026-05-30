package com.thedal.thedal_app.quartz;


import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.election.SurveyDownloadJobRepository;
import com.thedal.thedal_app.voter.VoterDownloadJobRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExportCleanupJob implements Job {

    @Autowired
    private VoterDownloadJobRepository voterDownloadJobRepository;

    @Autowired
    private SurveyDownloadJobRepository surveyDownloadJobRepository;

    @Autowired
    private AwsFileUpload awsFileUpload;

    @Value("${aws.s3.files.bucket}")
    private String s3Bucket;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        Long jobId = jobDataMap.getLong("jobId");
        String exportType = jobDataMap.getString("exportType"); // "VOTER" or "SURVEY"

        try {
            if ("VOTER".equals(exportType)) {
                voterDownloadJobRepository.findById(jobId).ifPresent(job -> {
                    String fileKey = "voter_exports/voter_export_" + jobId + ".xlsx";
                    deleteS3File(fileKey, jobId, "voter");
                    voterDownloadJobRepository.delete(job);
                    log.info("Deleted voter download job record for jobId: {}", jobId);
                });
            } else if ("SURVEY".equals(exportType)) {
                surveyDownloadJobRepository.findById(jobId).ifPresent(job -> {
                    String fileKey = "survey_exports/survey_export_" + jobId + ".xlsx";
                    deleteS3File(fileKey, jobId, "survey");
                    surveyDownloadJobRepository.delete(job);
                    log.info("Deleted survey download job record for jobId: {}", jobId);
                });
            } else {
                log.error("Invalid export type: {} for jobId: {}", exportType, jobId);
            }
        } catch (Exception e) {
            log.error("Failed to cleanup jobId: {} of type {}: {}", jobId, exportType, e.getMessage());
            throw new JobExecutionException("Failed to cleanup jobId: " + jobId, e);
        }
    }

    private void deleteS3File(String fileKey, Long jobId, String type) {
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
