package com.thedal.thedal_app.voter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.election.SurveyDownloadJob;
import com.thedal.thedal_app.election.SurveyDownloadJobRepository;
import com.thedal.thedal_app.election.SurveyFormEntity;
import com.thedal.thedal_app.election.SurveyFormRepository;
import com.thedal.thedal_app.election.SurveyFormService;
import com.thedal.thedal_app.report.pollday.export.PollDayExportJob;
import com.thedal.thedal_app.report.pollday.export.PollDayExportJobRepository;
import com.thedal.thedal_app.volunteer.VolunteerDownloadJob;
import com.thedal.thedal_app.volunteer.VolunteerDownloadJobRepository;
import com.thedal.thedal_app.voter.dto.ExportJobsResponse;
import com.thedal.thedal_app.voter.dto.VoterExportJobsResponse;
import com.thedal.thedal_app.voter.dto.VoterExportStatusResponse;

@Service
public class ExportService {

    @Autowired
    private VoterService voterService;

    @Autowired
    private SurveyFormService surveyFormService;

    @Autowired
    private SurveyFormRepository surveyFormRepository;

    @Autowired
    private VoterDownloadJobRepository voterDownloadJobRepository;

    @Autowired
    private SurveyDownloadJobRepository surveyDownloadJobRepository;

    @Autowired
    private PollDayExportJobRepository pollDayExportJobRepository;

    @Autowired
    private VolunteerDownloadJobRepository volunteerDownloadJobRepository;

    @Autowired
    private com.thedal.thedal_app.sirreport.SirReportExportJobRepository sirReportExportJobRepository;

    @Transactional
    public ExportJobsResponse getAllExportJobs(Long accountId, Long electionId, String type, String status,
                                              LocalDateTime startDate, LocalDateTime endDate) {
        List<ExportJobDTO> allJobs = new ArrayList<>();
        long totalCount = 0;

        // Fetch voter export jobs (if type is null or "VOTER")
        if (type == null || "VOTER".equalsIgnoreCase(type)) {
            VoterExportJobsResponse voterJobs = voterService.getExportJobsByElection(accountId, electionId, status, startDate, endDate);
            List<ExportJobDTO> voterDTOs = voterJobs.getExports().stream()
                    .map(job -> new ExportJobDTO(
                            job.getJobId(),
                            electionId,
                            "VOTER",
                            null,
                            null,
                            job.getStatus(),
                            convertToIST(job.getTimeStarted()),
                            convertToIST(job.getTimeCompleted()),
                            job.getAwsS3DownloadUrl(),
                            job.getMessage()))
                    .collect(Collectors.toList());
            allJobs.addAll(voterDTOs);
            totalCount += voterJobs.getTotalCount();
        }

        // Fetch survey export jobs (if type is null or "SURVEY")
        if (type == null || "SURVEY".equalsIgnoreCase(type)) {
            Specification<SurveyDownloadJob> surveySpec = Specification.where(SurveyDownloadJobSpecifications.hasAccountId(accountId))
                    .and(SurveyDownloadJobSpecifications.hasElectionId(electionId))
                    .and(SurveyDownloadJobSpecifications.hasTimeStartedBetween(startDate, endDate));
            if (status != null) {
                surveySpec = surveySpec.and(SurveyDownloadJobSpecifications.hasStatus(status));
            }
            List<SurveyDownloadJob> surveyJobs = surveyDownloadJobRepository.findAll(surveySpec);
            List<ExportJobDTO> surveyDTOs = surveyJobs.stream()
                    .map(job -> {
                        String formName = surveyFormRepository.findById(job.getFormId())
                                .map(SurveyFormEntity::getFormName)
                                .orElse("Unknown Form");
                        String message = generateMessage(job);
                        return new ExportJobDTO(
                                job.getId(),
                                electionId,
                                "SURVEY",
                                job.getFormId(),
                                formName,
                                job.getStatus(),
                                convertToIST(job.getTimeStarted()),
                                convertToIST(job.getTimeCompleted()),
                                job.getAwsS3DownloadUrl(),
                                message);
                    })
                    .collect(Collectors.toList());
            allJobs.addAll(surveyDTOs);
            totalCount += surveyJobs.size();
        }

        // Fetch poll day export jobs (if type is null or "POLL_DAY")
        if (type == null || "POLL_DAY".equalsIgnoreCase(type)) {
            List<PollDayExportJob> pollDayJobs = pollDayExportJobRepository
                    .findByAccountIdAndElectionIdOrderByCreatedAtDesc(accountId, electionId);
            
            // Apply filters to poll day jobs
            if (startDate != null || endDate != null || status != null) {
                pollDayJobs = pollDayJobs.stream()
                        .filter(job -> {
                            boolean matchesDateRange = true;
                            if (startDate != null) {
                                matchesDateRange = job.getCreatedAt() != null && !job.getCreatedAt().isBefore(startDate);
                            }
                            if (endDate != null && matchesDateRange) {
                                matchesDateRange = job.getCreatedAt() != null && !job.getCreatedAt().isAfter(endDate);
                            }
                            boolean matchesStatus = status == null || job.getStatus().name().equals(status);
                            return matchesDateRange && matchesStatus;
                        })
                        .collect(Collectors.toList());
            }

            List<ExportJobDTO> pollDayDTOs = pollDayJobs.stream()
                    .map(job -> {
                        String message = generatePollDayMessage(job);
                        return new ExportJobDTO(
                                job.getId(),
                                electionId,
                                "POLL_DAY",
                                job.getChartType().name(),
                                job.getFormat().name(),
                                job.getRowCount(),
                                job.getStatus().name(),
                                convertToIST(job.getCreatedAt()),
                                convertToIST(job.getFinishedAt()),
                                job.getS3Url(),
                                message);
                    })
                    .collect(Collectors.toList());
            allJobs.addAll(pollDayDTOs);
            totalCount += pollDayJobs.size();
        }

        // Fetch volunteer export jobs (if type is null or "VOLUNTEER" or "CADRE")
        if (type == null || "VOLUNTEER".equalsIgnoreCase(type) || "CADRE".equalsIgnoreCase(type)) {
            List<VolunteerDownloadJob> volunteerJobs = volunteerDownloadJobRepository.findAll().stream()
                    .filter(job -> job.getAccountId().equals(accountId) && job.getElectionId().equals(electionId))
                    .filter(job -> {
                        boolean matchesDateRange = true;
                        if (startDate != null) {
                            matchesDateRange = job.getTimeStarted() != null && !job.getTimeStarted().isBefore(startDate);
                        }
                        if (endDate != null && matchesDateRange) {
                            matchesDateRange = job.getTimeStarted() != null && !job.getTimeStarted().isAfter(endDate);
                        }
                        boolean matchesStatus = status == null || job.getStatus().equals(status);
                        return matchesDateRange && matchesStatus;
                    })
                    .collect(Collectors.toList());

            List<ExportJobDTO> volunteerDTOs = volunteerJobs.stream()
                    .map(job -> {
                        String message = generateVolunteerMessage(job);
                        return new ExportJobDTO(
                                job.getId(),
                                electionId,
                                "VOLUNTEER",
                                null,
                                null,
                                job.getStatus(),
                                convertToIST(job.getTimeStarted()),
                                convertToIST(job.getTimeCompleted()),
                                job.getAwsS3DownloadUrl(),
                                message);
                    })
                    .collect(Collectors.toList());
            allJobs.addAll(volunteerDTOs);
            totalCount += volunteerJobs.size();
        }

        // Fetch SIR report export jobs (if type is null or "SIR_REPORT")
        if (type == null || "SIR_REPORT".equalsIgnoreCase(type)) {
            List<com.thedal.thedal_app.sirreport.SirReportExportJob> sirJobs = 
                    sirReportExportJobRepository.findByAccountIdAndElectionIdOrderByTimeStartedDesc(accountId, electionId);
            
            // Apply filters
            if (startDate != null || endDate != null || status != null) {
                sirJobs = sirJobs.stream()
                        .filter(job -> {
                            boolean matchesDateRange = true;
                            if (startDate != null) {
                                matchesDateRange = job.getTimeStarted() != null && !job.getTimeStarted().isBefore(startDate);
                            }
                            if (endDate != null && matchesDateRange) {
                                matchesDateRange = job.getTimeStarted() != null && !job.getTimeStarted().isAfter(endDate);
                            }
                            boolean matchesStatus = status == null || job.getStatus().equals(status);
                            return matchesDateRange && matchesStatus;
                        })
                        .collect(Collectors.toList());
            }

            List<ExportJobDTO> sirDTOs = sirJobs.stream()
                    .map(job -> {
                        String message = String.format("%s Export (%s)", 
                                job.getExportType().name(), 
                                job.getFormat().name());
                        return new ExportJobDTO(
                                job.getId(),
                                electionId,
                                "SIR_REPORT",
                                job.getExportType().name(),
                                job.getFormat().name(),
                                job.getRecordCount(),
                                job.getStatus(),
                                convertToIST(job.getTimeStarted()),
                                convertToIST(job.getTimeCompleted()),
                                job.getAwsS3DownloadUrl(),
                                message);
                    })
                    .collect(Collectors.toList());
            allJobs.addAll(sirDTOs);
            totalCount += sirJobs.size();
        }

        // Sort all jobs by timeStarted
        allJobs.sort(Comparator.comparing(ExportJobDTO::getTimeStarted, Comparator.nullsLast(Comparator.reverseOrder())));

        return new ExportJobsResponse(allJobs, totalCount);
    }
    
    @Transactional
    public Page<ExportJobDTO> getAllExportJobsPaginated(Long accountId, Long electionId, String type, String status,
                                                        LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        List<ExportJobDTO> allJobs = new ArrayList<>();
        long totalCount = 0;

        System.out.println("DEBUG PAGINATED: accountId=" + accountId + ", electionId=" + electionId + ", type=" + type + ", status=" + status);
        System.out.println("DEBUG PAGINATED: startDate=" + startDate + ", endDate=" + endDate);

        // Fetch voter export jobs (if type is null or "VOTER")
        if (type == null || "VOTER".equalsIgnoreCase(type)) {
            Page<VoterExportStatusResponse> voterPage = voterService.getExportJobsByElectionPaginated(
                    accountId, electionId, status, startDate, endDate, pageable);
            List<ExportJobDTO> voterDTOs = voterPage.getContent().stream()
                    .map(job -> new ExportJobDTO(
                            job.getJobId(),
                            electionId,
                            "VOTER",
                            null,
                            null,
                            job.getStatus(),
                            convertToIST(job.getTimeStarted()),
                            convertToIST(job.getTimeCompleted()),
                            job.getAwsS3DownloadUrl(),
                            job.getMessage()))
                    .collect(Collectors.toList());
            allJobs.addAll(voterDTOs);
            totalCount += voterPage.getTotalElements();
        }

        // Fetch survey export jobs (if type is null or "SURVEY")
        if (type == null || "SURVEY".equalsIgnoreCase(type)) {
            Specification<SurveyDownloadJob> surveySpec = Specification.where(SurveyDownloadJobSpecifications.hasAccountId(accountId))
                    .and(SurveyDownloadJobSpecifications.hasElectionId(electionId))
                    .and(SurveyDownloadJobSpecifications.hasTimeStartedBetween(startDate, endDate));
            if (status != null) {
                surveySpec = surveySpec.and(SurveyDownloadJobSpecifications.hasStatus(status));
            }
            Page<SurveyDownloadJob> surveyPage = surveyDownloadJobRepository.findAll(surveySpec, pageable);
            List<ExportJobDTO> surveyDTOs = surveyPage.getContent().stream()
                    .map(job -> {
                        String formName = surveyFormRepository.findById(job.getFormId())
                                .map(SurveyFormEntity::getFormName)
                                .orElse("Unknown Form");
                        String message = generateMessage(job);
                        return new ExportJobDTO(
                                job.getId(),
                                electionId,
                                "SURVEY",
                                job.getFormId(),
                                formName,
                                job.getStatus(),
                                convertToIST(job.getTimeStarted()),
                                convertToIST(job.getTimeCompleted()),
                                job.getAwsS3DownloadUrl(),
                                message);
                    })
                    .collect(Collectors.toList());
            allJobs.addAll(surveyDTOs);
            totalCount += surveyPage.getTotalElements();
        }

        // Fetch poll day export jobs (if type is null or "POLL_DAY")
        if (type == null || "POLL_DAY".equalsIgnoreCase(type)) {
            List<PollDayExportJob> pollDayJobs = pollDayExportJobRepository
                    .findByAccountIdAndElectionIdOrderByCreatedAtDesc(accountId, electionId);
            
            // Apply filters to poll day jobs
            if (startDate != null || endDate != null || status != null) {
                pollDayJobs = pollDayJobs.stream()
                        .filter(job -> {
                            boolean matchesDateRange = true;
                            if (startDate != null) {
                                matchesDateRange = job.getCreatedAt() != null && !job.getCreatedAt().isBefore(startDate);
                            }
                            if (endDate != null && matchesDateRange) {
                                matchesDateRange = job.getCreatedAt() != null && !job.getCreatedAt().isAfter(endDate);
                            }
                            boolean matchesStatus = status == null || job.getStatus().name().equals(status);
                            return matchesDateRange && matchesStatus;
                        })
                        .collect(Collectors.toList());
            }

            List<ExportJobDTO> pollDayDTOs = pollDayJobs.stream()
                    .map(job -> {
                        String message = generatePollDayMessage(job);
                        return new ExportJobDTO(
                                job.getId(),
                                electionId,
                                "POLL_DAY",
                                job.getChartType().name(),
                                job.getFormat().name(),
                                job.getRowCount(),
                                job.getStatus().name(),
                                convertToIST(job.getCreatedAt()),
                                convertToIST(job.getFinishedAt()),
                                job.getS3Url(),
                                message);
                    })
                    .collect(Collectors.toList());
            allJobs.addAll(pollDayDTOs);
            totalCount += pollDayJobs.size();
        }

        // Fetch volunteer export jobs (if type is null or "VOLUNTEER" or "CADRE")
        if (type == null || "VOLUNTEER".equalsIgnoreCase(type) || "CADRE".equalsIgnoreCase(type)) {
            List<VolunteerDownloadJob> volunteerJobs = volunteerDownloadJobRepository.findAll().stream()
                    .filter(job -> job.getAccountId().equals(accountId) && job.getElectionId().equals(electionId))
                    .filter(job -> {
                        boolean matchesDateRange = true;
                        if (startDate != null) {
                            matchesDateRange = job.getTimeStarted() != null && !job.getTimeStarted().isBefore(startDate);
                        }
                        if (endDate != null && matchesDateRange) {
                            matchesDateRange = job.getTimeStarted() != null && !job.getTimeStarted().isAfter(endDate);
                        }
                        boolean matchesStatus = status == null || job.getStatus().equals(status);
                        return matchesDateRange && matchesStatus;
                    })
                    .collect(Collectors.toList());

            List<ExportJobDTO> volunteerDTOs = volunteerJobs.stream()
                    .map(job -> {
                        String message = generateVolunteerMessage(job);
                        return new ExportJobDTO(
                                job.getId(),
                                electionId,
                                "VOLUNTEER",
                                null,
                                null,
                                job.getStatus(),
                                convertToIST(job.getTimeStarted()),
                                convertToIST(job.getTimeCompleted()),
                                job.getAwsS3DownloadUrl(),
                                message);
                    })
                    .collect(Collectors.toList());
            allJobs.addAll(volunteerDTOs);
            totalCount += volunteerJobs.size();
        }

        // Fetch SIR report export jobs (if type is null or "SIR_REPORT")
        if (type == null || "SIR_REPORT".equalsIgnoreCase(type)) {
            System.out.println("DEBUG PAGINATED: Fetching SIR jobs...");
            List<com.thedal.thedal_app.sirreport.SirReportExportJob> sirJobs = 
                    sirReportExportJobRepository.findByAccountIdAndElectionIdOrderByTimeStartedDesc(accountId, electionId);
            
            System.out.println("DEBUG PAGINATED: Found " + sirJobs.size() + " SIR jobs");
            
            // Apply filters
            if (startDate != null || endDate != null || status != null) {
                sirJobs = sirJobs.stream()
                        .filter(job -> {
                            boolean matchesDateRange = true;
                            if (startDate != null) {
                                matchesDateRange = job.getTimeStarted() != null && !job.getTimeStarted().isBefore(startDate);
                            }
                            if (endDate != null && matchesDateRange) {
                                matchesDateRange = job.getTimeStarted() != null && !job.getTimeStarted().isAfter(endDate);
                            }
                            boolean matchesStatus = status == null || job.getStatus().equals(status);
                            return matchesDateRange && matchesStatus;
                        })
                        .collect(Collectors.toList());
                System.out.println("DEBUG PAGINATED: After filtering: " + sirJobs.size() + " SIR jobs");
            }

            List<ExportJobDTO> sirDTOs = sirJobs.stream()
                    .map(job -> {
                        String message = String.format("%s Export (%s)", 
                                job.getExportType().name(), 
                                job.getFormat().name());
                        return new ExportJobDTO(
                                job.getId(),
                                electionId,
                                "SIR_REPORT",
                                job.getExportType().name(),
                                job.getFormat().name(),
                                job.getRecordCount(),
                                job.getStatus(),
                                convertToIST(job.getTimeStarted()),
                                convertToIST(job.getTimeCompleted()),
                                job.getAwsS3DownloadUrl(),
                                message);
                    })
                    .collect(Collectors.toList());
            allJobs.addAll(sirDTOs);
            totalCount += sirJobs.size();
            System.out.println("DEBUG PAGINATED: Added " + sirDTOs.size() + " SIR DTOs. Total jobs: " + allJobs.size());
        }

        // Sort and apply pagination
        allJobs.sort(Comparator.comparing(ExportJobDTO::getTimeStarted, Comparator.nullsLast(Comparator.reverseOrder())));

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allJobs.size());
        List<ExportJobDTO> pagedJobs = allJobs.subList(Math.min(start, allJobs.size()), end);

        return new PageImpl<>(pagedJobs, pageable, totalCount);
    }
    
    private String generateMessage(SurveyDownloadJob job) {
        return switch (job.getStatus()) {
            case "IN_PROGRESS" -> "Export is in progress. Please check back later.";
            case "COMPLETED" -> {
                String message = "Export completed successfully. You can download the file.";
                if (job.getTimeCompleted() != null &&
                        job.getTimeCompleted().isBefore(LocalDateTime.now().minusHours(23))) {
                    message += " Note: This file and job will be deleted within the next hour.";
                }
                yield message;
            }
            case "FAILED" -> "Export failed: " + (job.getErrorMessage() != null ?
                    job.getErrorMessage() : "Please try again.");
            default -> "Unknown status.";
        };
    }

    private String generatePollDayMessage(PollDayExportJob job) {
        return switch (job.getStatus()) {
            case PENDING -> "Export is queued for processing.";
            case RUNNING -> "Export is in progress. Please check back later.";
            case COMPLETED -> {
                String message = String.format("Export completed successfully. %d records exported.",
                        job.getRowCount() != null ? job.getRowCount() : 0);
                if (job.getFinishedAt() != null &&
                        job.getFinishedAt().isBefore(LocalDateTime.now().minusHours(23))) {
                    message += " Note: This file will be deleted within the next hour.";
                }
                yield message;
            }
            case FAILED -> "Export failed: " + (job.getErrorMessage() != null ?
                    job.getErrorMessage() : "Please try again.");
        };
    }

    private String generateVolunteerMessage(VolunteerDownloadJob job) {
        return switch (job.getStatus()) {
            case "IN_PROGRESS" -> "Export is in progress. Please check back later.";
            case "COMPLETED" -> {
                String message = "Export completed successfully. You can download the file.";
                if (job.getTimeCompleted() != null &&
                        job.getTimeCompleted().isBefore(LocalDateTime.now().minusHours(23))) {
                    message += " Note: This file and job will be deleted within the next hour.";
                }
                yield message;
            }
            case "FAILED" -> "Export failed: " + (job.getErrorMessage() != null ?
                    job.getErrorMessage() : "Please try again.");
            default -> "Unknown status.";
        };
    }

    private LocalDateTime convertToIST(LocalDateTime utcTime) {
        if (utcTime == null) {
            return null;
        }
        return utcTime.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
                .toLocalDateTime();
    }
}