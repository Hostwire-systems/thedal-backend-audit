package com.thedal.thedal_app.quartz;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.VoterExportCleanupJob;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JobSchedulerService {

	 @Autowired
	    private Scheduler scheduler;

public void scheduleVoterFileUploadJob(Long bulkUploadId, Long accountId, Long electionId, Long fileId,
                                           Map<String, Integer> headerMapping, Set<String> mandatoryHeaders) {
        try {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("fileId", fileId);
            jobDataMap.put("accountId", accountId);
            jobDataMap.put("electionId", electionId);
            jobDataMap.put("bulkUploadId", bulkUploadId);
            jobDataMap.put("headerMapping", headerMapping);
            jobDataMap.put("mandatoryHeaders", mandatoryHeaders);

            JobDetail jobDetail = JobBuilder.newJob(VotersFileProcessJob.class)
                    .withIdentity("voterFileUploadJob_file_id_" + fileId, "voterBulkUploadGroup")
                    .withDescription("Voter Bulk upload")
                    .usingJobData(jobDataMap)
                    .storeDurably(true)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .forJob(jobDetail)
                    .withIdentity("oneTimeTrigger_file_id_" + fileId, "voterBulkUploadGroup")
                    .startAt(new Date())
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled voter file upload job");
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public boolean isJobCompleted(Long fileId) {
        try {
            String jobKey = "voterFileUploadJob_file_id_" + fileId;
            String groupKey = "voterBulkUploadGroup";

            if (scheduler.checkExists(new JobKey(jobKey, groupKey))) {
                log.info("Job exists for fileId: {}", fileId);
                return false;
            } else {
                log.info("Job does not exist for fileId: {}", fileId);
                return true;
            }
        } catch (SchedulerException e) {
            log.error("Error while checking job completion for fileId: {}", fileId, e);
            return false;
        }
    }

    public void scheduleVoterExportJob(Long jobId, Long accountId, Long electionId,
    		List<Integer> partNos, String gender, Integer minAge, Integer maxAge, Integer limit) {
        log.info("QUARTZ_SCHEDULER: Scheduling voter export job for jobId: {}, accountId: {}, electionId: {}, partNos: {}, gender: {}, minAge: {}, maxAge: {}, limit: {}", 
        		jobId, accountId, electionId, partNos, gender, minAge, maxAge, limit);
        
        try {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("jobId", jobId);
            jobDataMap.put("accountId", accountId);
            jobDataMap.put("electionId", electionId);
            //jobDataMap.put("partNo", partNo);
            jobDataMap.put("partNos", partNos);
            jobDataMap.put("gender", gender);
            jobDataMap.put("minAge", minAge);
            jobDataMap.put("maxAge", maxAge);
            jobDataMap.put("limit", limit);

            log.info("QUARTZ_SCHEDULER: Created JobDataMap for jobId: {}", jobId);

            JobDetail jobDetail = JobBuilder.newJob(VoterExportJob.class)
                    .withIdentity("voterExportJob_" + jobId, "voterExportGroup")
                    .usingJobData(jobDataMap)
                    .storeDurably(true)
                    .build();

            log.info("QUARTZ_SCHEDULER: Created JobDetail for jobId: {}, identity: {}", jobId, jobDetail.getKey());

            Trigger trigger = TriggerBuilder.newTrigger()
                    .forJob(jobDetail)
                    .withIdentity("voterExportTrigger_" + jobId, "voterExportGroup")
                    .startAt(new Date())
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow())
                    .build();

            log.info("QUARTZ_SCHEDULER: Created Trigger for jobId: {}, triggerKey: {}, startTime: {}", 
            		jobId, trigger.getKey(), trigger.getStartTime());

            scheduler.scheduleJob(jobDetail, trigger);
            
            log.info("QUARTZ_SCHEDULER: Successfully scheduled job for jobId: {}", jobId);
            
        } catch (SchedulerException e) {
            log.error("QUARTZ_SCHEDULER: Failed to schedule job for jobId: {}, error: {}", jobId, e.getMessage(), e);
            throw new ThedalException(ThedalError.SCHEDULING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void scheduleMemberFileUploadJob(Long bulkUploadId, Long accountId, Long electionId, Long fileId,
                                           Map<String, Integer> headerMapping, Set<String> mandatoryHeaders) {
        try {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("fileId", fileId);
            jobDataMap.put("accountId", accountId);
            jobDataMap.put("electionId", electionId);
            jobDataMap.put("bulkUploadId", bulkUploadId);
            jobDataMap.put("headerMapping", headerMapping);
            jobDataMap.put("mandatoryHeaders", mandatoryHeaders);

            JobDetail jobDetail = JobBuilder.newJob(MembersFileProcessJob.class)
                    .withIdentity("memberFileUploadJob_file_id_" + fileId, "memberBulkUploadGroup")
                    .usingJobData(jobDataMap)
                    .storeDurably(true)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .forJob(jobDetail)
                    .withIdentity("oneTimeTrigger_file_id_" + fileId, "memberBulkUploadGroup")
                    .startAt(new Date())
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled member file upload job");
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
    
//    public void scheduleSurveyExportJob(Long jobId, Long accountId, Long electionId,
//            Long formId, Integer limit) {
//      try {
//    	  JobDataMap jobDataMap = new JobDataMap();
//          jobDataMap.put("jobId", jobId);
//          jobDataMap.put("accountId", accountId);
//          jobDataMap.put("electionId", electionId);
//          jobDataMap.put("formId", formId);
//          //jobDataMap.put("voterIds", voterIds);
//          jobDataMap.put("limit", limit);
//
//          JobDetail jobDetail = JobBuilder.newJob(SurveyExportJob.class)
//                  .withIdentity("surveyExportJob_" + jobId, "surveyExportGroup")
//                  .usingJobData(jobDataMap)
//                  .storeDurably(true)
//                  .build();
//
//          Trigger trigger = TriggerBuilder.newTrigger()
//                  .forJob(jobDetail)
//                  .withIdentity("surveyExportTrigger_" + jobId, "surveyExportGroup")
//                  .startAt(new Date())
//                  .withSchedule(SimpleScheduleBuilder.simpleSchedule()
//                          .withMisfireHandlingInstructionFireNow())
//                  .build();
//
//          scheduler.scheduleJob(jobDetail, trigger);
//      } catch (SchedulerException e) {
//          throw new ThedalException(ThedalError.SCHEDULING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
//      }
//  }
    
    public void scheduleSurveyExportJob(Long jobId, Long accountId, Long electionId,
            Long formId, Integer limit) {
      try {
    	  JobDataMap jobDataMap = new JobDataMap();
          jobDataMap.put("jobId", jobId);
          jobDataMap.put("accountId", accountId);
          jobDataMap.put("electionId", electionId);
          jobDataMap.put("formId", formId);
          //jobDataMap.put("voterIds", voterIds);
          jobDataMap.put("limit", limit);

          JobDetail jobDetail = JobBuilder.newJob(SurveyExportJob.class)
                  .withIdentity("surveyExportJob_" + jobId, "surveyExportGroup")
                  .usingJobData(jobDataMap)
                  .storeDurably(true)
                  .build();

          Trigger trigger = TriggerBuilder.newTrigger()
                  .forJob(jobDetail)
                  .withIdentity("surveyExportTrigger_" + jobId, "surveyExportGroup")
                  .startAt(new Date())
                  .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                          .withMisfireHandlingInstructionFireNow())
                  .build();

          scheduler.scheduleJob(jobDetail, trigger);
      } catch (SchedulerException e) {
          throw new ThedalException(ThedalError.SCHEDULING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }


    public void scheduleVoterExportCleanupJob(Long jobId) {
        try {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("jobId", jobId);

            JobDetail jobDetail = JobBuilder.newJob(VoterExportCleanupJob.class)
                    .withIdentity("voterExportCleanupJob_" + jobId, "voterExportCleanupGroup")
                    .usingJobData(jobDataMap)
                    .storeDurably(true)
                    .build();

            // Schedule to run 24 hours from now
            Date startTime = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // 24 hours in milliseconds
            Trigger trigger = TriggerBuilder.newTrigger()
                    .forJob(jobDetail)
                    .withIdentity("voterExportCleanupTrigger_" + jobId, "voterExportCleanupGroup")
                    .startAt(startTime)
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow())
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled voter export cleanup job for jobId: {}", jobId);
        } catch (SchedulerException e) {
            log.error("Failed to schedule cleanup job for jobId: {}", jobId, e);
            throw new ThedalException(ThedalError.SCHEDULING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    

    public void scheduleSurveyExportCleanupJob(Long jobId) {
        scheduleExportCleanupJob(jobId, "SURVEY", "surveyExportCleanupGroup", "surveyExportCleanupTrigger_");
    }

    private void scheduleExportCleanupJob(Long jobId, String exportType, String group, String triggerPrefix) {
        try {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("jobId", jobId);
            jobDataMap.put("exportType", exportType);

            JobDetail jobDetail = JobBuilder.newJob(ExportCleanupJob.class)
                    .withIdentity("exportCleanupJob_" + jobId, group)
                    .usingJobData(jobDataMap)
                    .storeDurably(true)
                    .build();

            Date startTime = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            Trigger trigger = TriggerBuilder.newTrigger()
                    .forJob(jobDetail)
                    .withIdentity(triggerPrefix + jobId, group)
                    .startAt(startTime)
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow())
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled {} export cleanup job for jobId: {}", exportType, jobId);
        } catch (SchedulerException e) {
            log.error("Failed to schedule cleanup job for jobId: {}", jobId, e);
            throw new ThedalException(ThedalError.SCHEDULING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Schedule part manager export job
     */
    public void schedulePartManagerExportJob(Long jobId, Long accountId, Long electionId, String format) {
        log.info("QUARTZ_SCHEDULER: Scheduling part manager export job for jobId: {}, accountId: {}, electionId: {}, format: {}", 
                jobId, accountId, electionId, format);
        
        try {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("jobId", jobId);
            jobDataMap.put("accountId", accountId);
            jobDataMap.put("electionId", electionId);
            jobDataMap.put("format", format);
            
            log.info("QUARTZ_SCHEDULER: Created JobDataMap for part manager export jobId: {}", jobId);
            
            JobDetail jobDetail = JobBuilder.newJob(PartManagerExportJob.class)
                    .withIdentity("partManagerExportJob_" + jobId, "partManagerExportGroup")
                    .withDescription("Part Manager Export - Format: " + format)
                    .usingJobData(jobDataMap)
                    .storeDurably(true)
                    .build();
            
            log.info("QUARTZ_SCHEDULER: Created JobDetail for part manager export jobId: {}", jobId);
            
            Trigger trigger = TriggerBuilder.newTrigger()
                    .forJob(jobDetail)
                    .withIdentity("partManagerExportTrigger_" + jobId, "partManagerExportGroup")
                    .startAt(new Date())
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow())
                    .build();
            
            log.info("QUARTZ_SCHEDULER: Created Trigger for part manager export jobId: {}", jobId);
            
            scheduler.scheduleJob(jobDetail, trigger);
            
            log.info("QUARTZ_SCHEDULER: Successfully scheduled part manager export job for jobId: {}", jobId);
        } catch (SchedulerException e) {
            log.error("QUARTZ_SCHEDULER: Failed to schedule part manager export job for jobId: {}", jobId, e);
            throw new ThedalException(ThedalError.SCHEDULING_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


	 
}