package com.thedal.thedal_app.voter;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class VoterExportCleanupJob implements Job {
    
	@Autowired
    private VoterDownloadJobRepository voterDownloadJobRepository;

    @Autowired
    private AwsFileUpload awsFileUpload;

    @Value("${aws.s3.files.bucket}")
    private String s3Bucket;

    @Override
    public void execute(JobExecutionContext context) {
        Long jobId = context.getJobDetail().getJobDataMap().getLong("jobId");
        log.info("Starting cleanup for voter export job {}", jobId);

        try {
            VoterDownloadJob job = voterDownloadJobRepository.findById(jobId)
                    .orElseThrow(() -> new ThedalException(ThedalError.JOB_NOT_FOUND, HttpStatus.NOT_FOUND));

            // Delete S3 file if it exists
            if (job.getAwsS3DownloadUrl() != null) {
                String fileKey = extractS3FileKey(job.getAwsS3DownloadUrl());
                try {
                    awsFileUpload.deleteS3Object(s3Bucket, fileKey);
                    log.info("Deleted S3 file for job {}: {}", jobId, fileKey);
                } catch (Exception e) {
                    log.error("Failed to delete S3 file for job {}: {}", jobId, e.getMessage());
                    throw new ThedalException(ThedalError.S3_DELETE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            // Delete the job from the database
            voterDownloadJobRepository.delete(job);
            log.info("Deleted voter export job {} from database", jobId);

        } catch (Exception e) {
            log.error("Cleanup failed for job {}: {}", jobId, e.getMessage());
            throw new ThedalException(ThedalError.CLEANUP_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private String extractS3FileKey(String s3Url) {
        return s3Url.substring(s3Url.indexOf("voter_exports/"));
    }
}