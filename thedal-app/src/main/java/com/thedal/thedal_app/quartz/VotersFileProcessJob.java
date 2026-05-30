package com.thedal.thedal_app.quartz;

import java.util.Map;
import java.util.Set;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.thedal.thedal_app.voter.BulkUploadStatus;
import com.thedal.thedal_app.voter.VoterServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class VotersFileProcessJob implements Job {

private final VoterServiceImpl voterServiceImpl;
	
	public VotersFileProcessJob(VoterServiceImpl voterServiceImpl) {
		this.voterServiceImpl = voterServiceImpl;
	}
	
//	@Override
//	public void execute(JobExecutionContext context) throws JobExecutionException {
//		// Long accountId = (Long)
//		// context.getJobDetail().getJobDataMap().get("accountId");
//		Long electionId = (Long) context.getJobDetail().getJobDataMap().get("electionId");
//		Long fileId = (Long) context.getJobDetail().getJobDataMap().get("fileId");
//		Long bulkUploadId = (Long) context.getJobDetail().getJobDataMap().get("bulkUploadId");
//		Long accountId = (Long) context.getJobDetail().getJobDataMap().get("accountId");
////		@SuppressWarnings("unchecked")
////        List<String> headerErrors = (List<String>) context.getJobDetail().getJobDataMap().get("headerErrors");
////		
//		//voterServiceImpl.startUploadVotersFromXlsxOrCsv(accountId,bulkUploadId,electionId,fileId);
//		 try {
//		        voterServiceImpl.startUploadVotersFromXlsxOrCsv(accountId, bulkUploadId, electionId, fileId);
//		    } catch (Exception e) {
//		        // Update status to FAILED if an exception occurs
//		        voterServiceImpl.updateBulkUploadStatus(bulkUploadId, BulkUploadStatus.FAILED);
//		        throw new JobExecutionException(e);
//		    }
//	}
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
	    System.out.println("=== QUARTZ JOB STARTED ===");
	    
	    Long electionId = (Long) context.getJobDetail().getJobDataMap().get("electionId");
	    Long fileId = (Long) context.getJobDetail().getJobDataMap().get("fileId");
	    Long bulkUploadId = (Long) context.getJobDetail().getJobDataMap().get("bulkUploadId");
	    Long accountId = (Long) context.getJobDetail().getJobDataMap().get("accountId");
	    @SuppressWarnings("unchecked")
	    Map<String, Integer> headerMapping = (Map<String, Integer>) context.getJobDetail().getJobDataMap().get("headerMapping");
	    @SuppressWarnings("unchecked")
	    Set<String> mandatoryHeaders = (Set<String>) context.getJobDetail().getJobDataMap().get("mandatoryHeaders");

	    System.out.println("QUARTZ JOB PARAMS - bulkUploadId: " + bulkUploadId + ", accountId: " + accountId + 
	                      ", electionId: " + electionId + ", fileId: " + fileId);

	    try {
	        System.out.println("QUARTZ JOB - About to call voterServiceImpl.startUploadVotersFromXlsxOrCsv()");
	        voterServiceImpl.startUploadVotersFromXlsxOrCsv(accountId, bulkUploadId, electionId, fileId, headerMapping, mandatoryHeaders);
	        System.out.println("QUARTZ JOB - Successfully completed voterServiceImpl.startUploadVotersFromXlsxOrCsv()");
	    } catch (Exception e) {
	        System.err.println("QUARTZ JOB FAILED - Exception in job execution: " + e.getMessage());
	        e.printStackTrace();
	        voterServiceImpl.updateBulkUploadStatus(bulkUploadId, BulkUploadStatus.FAILED);
	        throw new JobExecutionException(e);
	    }
	    
	    System.out.println("=== QUARTZ JOB COMPLETED ===");
	}
	
	
	
}