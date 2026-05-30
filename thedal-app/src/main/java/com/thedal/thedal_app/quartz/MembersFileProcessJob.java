package com.thedal.thedal_app.quartz;

import java.util.Map;
import java.util.Set;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.thedal.thedal_app.voter.BulkUploadStatus;
import com.thedal.thedal_app.voter.MemberService;

@Component
public class MembersFileProcessJob implements Job {

	@Autowired
    private MemberService memberService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long electionId = (Long) context.getJobDetail().getJobDataMap().get("electionId");
        Long fileId = (Long) context.getJobDetail().getJobDataMap().get("fileId");
        Long bulkUploadId = (Long) context.getJobDetail().getJobDataMap().get("bulkUploadId");
        Long accountId = (Long) context.getJobDetail().getJobDataMap().get("accountId");
        @SuppressWarnings("unchecked")
        Map<String, Integer> headerMapping = (Map<String, Integer>) context.getJobDetail().getJobDataMap().get("headerMapping");
        @SuppressWarnings("unchecked")
        Set<String> mandatoryHeaders = (Set<String>) context.getJobDetail().getJobDataMap().get("mandatoryHeaders");

        try {
            memberService.startUploadMembersFromXlsxOrCsv(accountId, bulkUploadId, electionId, fileId, headerMapping, mandatoryHeaders);
        } catch (Exception e) {
            memberService.updateBulkUploadStatus(bulkUploadId, BulkUploadStatus.FAILED);
            throw new JobExecutionException(e);
        }
    }
}