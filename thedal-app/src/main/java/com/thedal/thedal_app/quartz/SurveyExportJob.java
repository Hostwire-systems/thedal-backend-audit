package com.thedal.thedal_app.quartz;

import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.thedal.thedal_app.election.SurveyFormService;

@Component
public class SurveyExportJob implements Job {

    @Autowired
    private SurveyFormService surveyFormService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long jobId = context.getJobDetail().getJobDataMap().getLong("jobId");
        Long accountId = context.getJobDetail().getJobDataMap().getLong("accountId");
        Long electionId = context.getJobDetail().getJobDataMap().getLong("electionId");
        Long formId = context.getJobDetail().getJobDataMap().getLong("formId");
        List<String> voterIds = (List<String>) context.getJobDetail().getJobDataMap().get("voterIds");
        Integer limit = (Integer) context.getJobDetail().getJobDataMap().get("limit");

        surveyFormService.processSurveyExport(jobId, accountId, electionId, formId, voterIds, limit);
    }
}