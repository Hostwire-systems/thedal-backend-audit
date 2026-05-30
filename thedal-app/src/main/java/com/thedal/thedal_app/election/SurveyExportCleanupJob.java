package com.thedal.thedal_app.election;


import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SurveyExportCleanupJob implements Job {

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

        try {
            surveyDownloadJobRepository.findById(jobId).ifPresent(job -> {
                // Delete S3 file
                if (job.getAwsS3DownloadUrl() != null) {
                    String fileKey = "survey_exports/survey_export_" + jobId + ".xlsx";
                    try {
                        awsFileUpload.deleteS3Object(s3Bucket, fileKey);
                        log.info("Deleted S3 file for survey jobId: {}", jobId);
                    } catch (Exception e) {
                        log.error("Failed to delete S3 file for survey jobId: {}: {}", jobId, e.getMessage());
                    }
                }

                // Delete database record
                surveyDownloadJobRepository.delete(job);
                log.info("Deleted survey download job record for jobId: {}", jobId);
            });
        } catch (Exception e) {
            log.error("Failed to cleanup survey jobId: {}: {}", jobId, e.getMessage());
            throw new JobExecutionException("Failed to cleanup survey jobId: " + jobId, e);
        }
    }
}
