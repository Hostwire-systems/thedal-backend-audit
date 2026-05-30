package com.thedal.thedal_app.report.pollday;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.report.pollday.dto.ChartConfig;
import com.thedal.thedal_app.report.pollday.dto.PollDayChartConfigRequest;
import com.thedal.thedal_app.report.pollday.dto.PollDayChartConfigResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollDayChartConfigService {

    private final PollDayChartConfigRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PollDayChartConfigResponse saveChartConfig(PollDayChartConfigRequest request) {
        try {
            log.info("Saving chart configuration for accountId={}, electionId={}", 
                request.getAccountId(), request.getElectionId());
            
            // Validate charts
            validateCharts(request.getCharts());
            
            // Convert charts to JSON
            String chartsJson = objectMapper.writeValueAsString(request.getCharts());
            
            // Find existing or create new
            Optional<PollDayChartConfig> existing = repository
                .findByAccountIdAndElectionId(request.getAccountId(), request.getElectionId());
            
            PollDayChartConfig entity;
            if (existing.isPresent()) {
                entity = existing.get();
                entity.setCharts(chartsJson);
                log.info("Updating existing chart configuration id={}", entity.getId());
            } else {
                entity = new PollDayChartConfig();
                entity.setAccountId(request.getAccountId());
                entity.setElectionId(request.getElectionId());
                entity.setCharts(chartsJson);
                log.info("Creating new chart configuration");
            }
            
            entity = repository.save(entity);
            
            return mapToResponse(entity);
            
        } catch (Exception e) {
            log.error("Error saving chart configuration for accountId={}, electionId={}", 
                request.getAccountId(), request.getElectionId(), e);
            throw new RuntimeException("Failed to save chart configuration: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<PollDayChartConfigResponse> getChartConfig(Long accountId, Long electionId) {
        try {
            log.info("Retrieving chart configuration for accountId={}, electionId={}", 
                accountId, electionId);
            
            Optional<PollDayChartConfig> entity = repository
                .findByAccountIdAndElectionId(accountId, electionId);
            
            if (entity.isPresent()) {
                log.info("Found chart configuration: id={}, accountId={}, electionId={}", 
                    entity.get().getId(), entity.get().getAccountId(), entity.get().getElectionId());
            } else {
                log.info("No chart configuration found for accountId={}, electionId={}", 
                    accountId, electionId);
            }
            
            return entity.map(this::mapToResponse);
            
        } catch (Exception e) {
            log.error("Error retrieving chart configuration for accountId={}, electionId={}", 
                accountId, electionId, e);
            throw new RuntimeException("Failed to retrieve chart configuration: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteChartConfig(Long accountId, Long electionId) {
        try {
            log.info("Deleting chart configuration for accountId={}, electionId={}", 
                accountId, electionId);
            
            repository.deleteByAccountIdAndElectionId(accountId, electionId);
            
            log.info("Successfully deleted chart configuration for accountId={}, electionId={}", 
                accountId, electionId);
            
        } catch (Exception e) {
            log.error("Error deleting chart configuration for accountId={}, electionId={}", 
                accountId, electionId, e);
            throw new RuntimeException("Failed to delete chart configuration: " + e.getMessage(), e);
        }
    }

    private void validateCharts(List<ChartConfig> charts) {
        if (charts == null || charts.isEmpty()) {
            throw new IllegalArgumentException("At least one chart is required");
        }
        
        if (charts.size() > 20) {
            throw new IllegalArgumentException("Maximum 20 charts allowed");
        }
        
        for (ChartConfig chart : charts) {
            String actualChartId = chart.getActualChartId();
            if (actualChartId == null || actualChartId.trim().isEmpty()) {
                throw new IllegalArgumentException("Chart ID is required for all charts");
            }
            
            // Normalize: set chartId if only id was provided
            if (chart.getChartId() == null && chart.getId() != null) {
                chart.setChartId(chart.getId());
            }
            
            if (chart.getSelectedParts() == null) {
                throw new IllegalArgumentException("selectedParts must not be null (use empty array for all parts)");
            }
            
            // Validate that all part numbers are non-negative
            for (Integer partNumber : chart.getSelectedParts()) {
                if (partNumber == null || partNumber < 0) {
                    throw new IllegalArgumentException("Invalid part number: " + partNumber);
                }
            }
            
            // Validate viewType if provided
            if (chart.getViewType() != null && !chart.getViewType().isEmpty()) {
                String viewType = chart.getViewType().toLowerCase();
                if (!viewType.equals("bar") && !viewType.equals("line") && !viewType.equals("table") && !viewType.equals("stacked")) {
                    throw new IllegalArgumentException("Invalid viewType: " + chart.getViewType() + ". Must be 'bar', 'line', 'table', or 'stacked'");
                }
            }
            
            // Validate sortType if provided
            if (chart.getSortType() != null && !chart.getSortType().isEmpty()) {
                String sortType = chart.getSortType().toLowerCase();
                if (!sortType.equals("asc") && !sortType.equals("desc")) {
                    throw new IllegalArgumentException("Invalid sortType: " + chart.getSortType() + ". Must be 'asc' or 'desc'");
                }
            }
            
            // Validate width if provided
            if (chart.getWidth() != null && chart.getWidth() < 100) {
                throw new IllegalArgumentException("Chart width must be at least 100 pixels");
            }
            
            // Validate height if provided
            if (chart.getHeight() != null && chart.getHeight() < 100) {
                throw new IllegalArgumentException("Chart height must be at least 100 pixels");
            }
            
            // Note: x and y positions are not validated as they may need negative values
            // for certain layout systems or positioning relative to containers
        }
    }

    private PollDayChartConfigResponse mapToResponse(PollDayChartConfig entity) {
        try {
            List<ChartConfig> charts = objectMapper.readValue(
                entity.getCharts(), 
                new TypeReference<List<ChartConfig>>() {}
            );
            
            return new PollDayChartConfigResponse(
                entity.getId(),
                entity.getAccountId(),
                entity.getElectionId(),
                charts,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
            );
        } catch (Exception e) {
            log.error("Error mapping entity to response", e);
            throw new RuntimeException("Failed to parse chart configuration", e);
        }
    }
}
