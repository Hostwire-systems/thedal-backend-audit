package com.thedal.thedal_app.quartz;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.thedal.thedal_app.election.PartManagerDownloadJob;
import com.thedal.thedal_app.election.PartManagerDownloadJobRepository;
import com.thedal.thedal_app.election.PartMangerService;

import java.time.LocalDateTime;

@Component
public class PartManagerExportJob implements Job {
    
    private static final Logger log = LoggerFactory.getLogger(PartManagerExportJob.class);
    
    @Autowired
    private PartMangerService partManagerService;
    
    @Autowired
    private PartManagerDownloadJobRepository partManagerDownloadJobRepository;
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        Long jobId = jobDataMap.getLong("jobId");
        Long accountId = jobDataMap.getLong("accountId");
        Long electionId = jobDataMap.getLong("electionId");
        String format = jobDataMap.getString("format");
        
        log.info("QUARTZ_JOB: Starting PartManagerExportJob for jobId: {}, format: {}", jobId, format);
        
        try {
            log.info("QUARTZ_JOB: Calling partManagerService.processPartManagerExport for jobId: {}", jobId);
            
            partManagerService.processPartManagerExport(jobId, accountId, electionId, format);
            
            log.info("QUARTZ_JOB: Successfully completed processPartManagerExport for jobId: {}", jobId);
            
        } catch (Exception e) {
            log.error("QUARTZ_JOB: Error executing PartManagerExportJob for jobId: {}, error: {}", 
                    jobId, e.getMessage(), e);
            
            try {
                PartManagerDownloadJob job = partManagerDownloadJobRepository.findById(jobId)
                        .orElseThrow(() -> new JobExecutionException("Job not found for jobId: " + jobId));
                job.setStatus("FAILED");
                job.setMessage("Export job failed: " + e.getMessage());
                job.setTimeCompleted(LocalDateTime.now());
                partManagerDownloadJobRepository.save(job);
            } catch (Exception ex) {
                log.error("QUARTZ_JOB: Failed to update job status to FAILED for jobId: {}", jobId, ex);
            }
            
            throw new JobExecutionException("Part manager export job failed", e);
        }
    }
}
