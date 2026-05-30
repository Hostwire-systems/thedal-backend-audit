package com.thedal.thedal_app.quartz;

import java.time.LocalDateTime;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.thedal.thedal_app.voter.VoterDownloadJob;
import com.thedal.thedal_app.voter.VoterDownloadJobRepository;
import com.thedal.thedal_app.voter.VoterServiceImpl;

@Component
public class VoterExportJob implements Job {

	private static final Logger log = LoggerFactory.getLogger(VoterExportJob.class);
	
     private final VoterServiceImpl voterServiceImpl;
	
	public VoterExportJob(VoterServiceImpl voterServiceImpl) {
		this.voterServiceImpl = voterServiceImpl;
	}
	@Autowired
    private VoterDownloadJobRepository voterDownloadJobRepository;

//    @Override
//    public void execute(JobExecutionContext context) throws JobExecutionException {
//        Long jobId = context.getJobDetail().getJobDataMap().getLong("jobId");
//        Long accountId = context.getJobDetail().getJobDataMap().getLong("accountId");
//        Long electionId = context.getJobDetail().getJobDataMap().getLong("electionId");
//        //Integer limit = context.getJobDetail().getJobDataMap().getInt("limit"); 
//
//        Integer limit = null;
//        if (context.getJobDetail().getJobDataMap().containsKey("limit")) {
//            Object limitObject = context.getJobDetail().getJobDataMap().get("limit");
//            if (limitObject instanceof Integer) {
//                limit = (Integer) limitObject;
//            } else if (limitObject instanceof String) {
//                try {
//                    limit = Integer.parseInt((String) limitObject);
//                } catch (NumberFormatException e) {
//                    limit = null; // Default to null if conversion fails
//                }
//            }
//        }
//        
//        //voterServiceImpl.processVoterExport(jobId, accountId, electionId);
//        voterServiceImpl.processVoterExport(jobId, accountId, electionId, limit);
//    }
	@Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        Long jobId = dataMap.getLong("jobId");
        Long accountId = dataMap.getLong("accountId");
        Long electionId = dataMap.getLong("electionId");
        
        log.info("QUARTZ_JOB: Starting VoterExportJob execution for jobId: {}, accountId: {}, electionId: {}", 
        		jobId, accountId, electionId);
        
        // Handle null values properly
        List<Integer> partNos = null;
        if (dataMap.containsKey("partNos") && dataMap.get("partNos") != null) {
            partNos = (List<Integer>) dataMap.get("partNos");
        }
        
        String gender = null;
        if (dataMap.containsKey("gender") && dataMap.get("gender") != null) {
            gender = dataMap.getString("gender");
        }
        
        Integer minAge = null;
        if (dataMap.containsKey("minAge") && dataMap.get("minAge") != null) {
            minAge = dataMap.getInt("minAge");
        }
        
        Integer maxAge = null;
        if (dataMap.containsKey("maxAge") && dataMap.get("maxAge") != null) {
            maxAge = dataMap.getInt("maxAge");
        }
        
        Integer limit = null;
        if (dataMap.containsKey("limit") && dataMap.get("limit") != null) {
            limit = dataMap.getInt("limit");
        }

        log.info("QUARTZ_JOB: Parsed job parameters for jobId: {} - partNos: {}, gender: {}, minAge: {}, maxAge: {}, limit: {}", 
        		jobId, partNos, gender, minAge, maxAge, limit);

        try {
            log.info("QUARTZ_JOB: Calling voterServiceImpl.processVoterExport for jobId: {}", jobId);
            
            // Use the main export method which now handles local storage
            voterServiceImpl.processVoterExport(jobId, accountId, electionId, 
            		partNos, gender, minAge, maxAge, limit);
            
            log.info("QUARTZ_JOB: Successfully completed processVoterExport for jobId: {}", jobId);
            
        } catch (Exception e) {
        	log.error("QUARTZ_JOB: Error executing VoterExportJob for jobId: {}, error: {}", jobId, e.getMessage(), e);
        	
        	try {
	        	VoterDownloadJob job = voterDownloadJobRepository.findById(jobId)
	                    .orElseThrow(() -> new JobExecutionException("Job not found for jobId: " + jobId));
	                job.setStatus("FAILED");
	                job.setErrorMessage("Job execution failed: " + e.getMessage());
	                job.setTimeCompleted(LocalDateTime.now());
	                voterDownloadJobRepository.save(job);
	                
	                log.info("QUARTZ_JOB: Updated job status to FAILED for jobId: {}", jobId);
        	} catch (Exception updateException) {
        		log.error("QUARTZ_JOB: Failed to update job status to FAILED for jobId: {}, updateError: {}", 
        				jobId, updateException.getMessage(), updateException);
        	}
        	
            throw new JobExecutionException("Failed to process voter export for jobId: " + jobId, e);
        }
    }
}