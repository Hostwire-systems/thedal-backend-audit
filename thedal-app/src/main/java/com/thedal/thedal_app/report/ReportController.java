package com.thedal.thedal_app.report;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.report.cadre.ElectionDashboardDTO;
import com.thedal.thedal_app.report.dto.BoothWiseTimingVotersCountResponseDTO;
import com.thedal.thedal_app.report.dto.CadreOverviewResponseDTO;
import com.thedal.thedal_app.report.dto.CadrePerformanceDto;
import com.thedal.thedal_app.report.dto.CadreReportDTO;
import com.thedal.thedal_app.report.dto.ElectionDashboardOverviewResponseDTO;
import com.thedal.thedal_app.report.dto.PollDayReportDTO;
import com.thedal.thedal_app.report.dto.PollingAgeWiseResponse;
import com.thedal.thedal_app.report.dto.PollingBasedOnAgeResponseDTO;
import com.thedal.thedal_app.report.dto.PollingPartyWiseResponse;
import com.thedal.thedal_app.report.dto.PartyWiseVotersResponse;
import com.thedal.thedal_app.report.dto.BoothStrengthOverviewDTO;
import com.thedal.thedal_app.report.dto.VotersHaveContactsResponseDTO;
import com.thedal.thedal_app.report.pollday.BoothWiseTimingVotersCount;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

@RestController("/reports")
@Slf4j
public class ReportController {

	@Autowired
	private ReportService reportService;
	@Autowired
    private RequestDetailsService requestDetails;
	
	
	@GetMapping("/cadre/overview/{electionId}")
	public CadreOverviewResponseDTO getCadreOverviewForElection(@PathVariable Long electionId){
		return reportService.getCadreOverviewForElection(electionId);
	}
	
	@GetMapping("/cadre/performance/top/{electionId}")
	public List<CadrePerformanceDto> getTopPerformanceCadre(@PathVariable Long electionId){
		return reportService.getTopPerformanceCadre(electionId);
	}
	
	@GetMapping("/cadre/performance/least/{electionId}")
	public List<CadrePerformanceDto> getLeastPerformanceCadre(@PathVariable Long electionId){
		return reportService.getLeastPerformanceCadre(electionId);
	}
	
	@GetMapping("/election/overview/{electionId}")
	public ElectionDashboardOverviewResponseDTO getElectionDashboardOverview(@PathVariable Long electionId){
		return reportService.getElectionDashboardOverview(electionId);
	}
	
	@GetMapping("/election/polling-based-age/{electionId}")
	public PollingBasedOnAgeResponseDTO getPollingBasedOnAgeRange(@PathVariable Long electionId){
		return reportService.getPollingBasedOnAgeRange(electionId);
	}
	
	@GetMapping("/election/voters-have-contacts/{electionId}")
	public List<VotersHaveContactsResponseDTO> getVotersHaveContacts(@PathVariable Long electionId){
		return reportService.getVotersHaveContacts(electionId);
	}
	
	@GetMapping("/election/polling-based-party/{electionId}")
	public PartyWiseVotersResponse getPollingBasedParty(@PathVariable Long electionId){
		return reportService.getPollingBasedParty(electionId);
	}
	
	@GetMapping("/election/booth-strength-overview/{electionId}")
	public BoothStrengthOverviewDTO getBoothStrengthOverview(@PathVariable Long electionId){
		return reportService.getBoothStrengthOverview(electionId);
	}
	
	  @GetMapping("/poll-day/election/polling-age-graph/{electionId}")
	    public ResponseEntity<PollingAgeWiseResponse> getAgeWiseVotersCountByElectionId(
	            @PathVariable Long electionId) {
		  PollingAgeWiseResponse res = reportService.getPollingAgeWiseData(electionId);
	        return ResponseEntity.ok(res);
	    }
	
    @GetMapping("/poll-day/election/polling-each-booth/{electionId}")
    public ResponseEntity<List<BoothWiseTimingVotersCountResponseDTO>> getListOfBoothWiseTimingVotersCountByElectionId(
            @PathVariable Long electionId) {
        List<BoothWiseTimingVotersCountResponseDTO> records = reportService.getBoothWiseTimingVotersCountByElectionId(electionId);
        return ResponseEntity.ok(records);
    }
    
//    @GetMapping("/poll-day/election/{electionId}/booth-wise-timing/{boothNumber}")
//    public ResponseEntity<BoothWiseTimingVotersCountRedis> getBoothWiseTimingVotersCountByElectionIdAndBoothNumber(
//            @PathVariable Long electionId,
//            @PathVariable Long boothNumber) {
//        BoothWiseTimingVotersCountRedis record = reportService.getBoothWiseTimingVotersCountByElectionIdAndBoothNumber(electionId, boothNumber);
//        if (record == null) {
//            return ResponseEntity.notFound().build();
//        }
//        return ResponseEntity.ok(record);
//    }
    @GetMapping("/poll-day/election/{electionId}/booth-wise-timing/{boothNumber}")
    public ResponseEntity<BoothWiseTimingVotersCount> getBoothWiseTimingVotersCountByElectionIdAndBoothNumber(
            @PathVariable Long electionId,
            @PathVariable Long boothNumber) {
        try {
            BoothWiseTimingVotersCount record = reportService.getBoothWiseTimingVotersCountByElectionIdAndBoothNumber(electionId, boothNumber);
            if (record == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(record);
        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing request for electionId={}, boothNumber={}: {}", electionId, boothNumber, e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
   
    @Operation(summary = "Get all users' performance", description = "Retrieves performance data for all users in the account")
    @GetMapping("/cadre/performance/all")
    //@PreAuthorize("hasRole('ADMIN')") // Restrict to admins only
    public List<CadrePerformanceDto> getAllUsersPerformance() {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        return reportService.getAllUsersPerformance(accountId);
    }
    
    @Operation(summary = "Update or create cadre report", description = "Updates an existing cadre report or creates a new one for a specific election")
    @PutMapping("/cadre/save/{electionId}")
    public ThedalResponse<Void> updateCadreReport(@PathVariable Long electionId, @RequestBody CadreReportDTO dto) {
        boolean isCreated = reportService.saveOrUpdateCadreReport(electionId,dto);
        //return new ThedalResponse()<>(isCreated ? HttpStatus.CREATED : HttpStatus.OK);
        return new ThedalResponse(ThedalSuccess.CADRE_REPORT_CREATED, HttpStatus.ACCEPTED);
            
    }

    @Operation(summary = "Get cadre reports", description = "Fetches all cadre reports for a specific election")
    @GetMapping("/cadre/reports/{electionId}")
    public ThedalResponse<List<CadreReportDTO>> getCadreReports(@PathVariable Long electionId) {
        List<CadreReportDTO> reports = reportService.getCadreReports(electionId);
        return new ThedalResponse<>(ThedalSuccess.CADRE_REPORTS_FETCHED, reports);
    }
    
//    @Operation(summary = "Update or create poll day report", description = "Updates an existing poll day report or creates a new one for a specific election")
//    @PutMapping("/pollday/save")
//    public ResponseEntity<Void> updatePollDayReport(@RequestBody PollDayReportDTO dto) {
//        boolean isCreated = reportService.saveOrUpdatePollDayReport(dto);
//        return new ResponseEntity<>(isCreated ? HttpStatus.CREATED : HttpStatus.OK);
//    }
    @Operation(summary = "Update or create poll day report", description = "Updates an existing poll day report or creates a new one for a specific election")
    @PutMapping("/pollday/save/{electionId}")
    public ThedalResponse<Void> updatePollDayReport(@PathVariable Long electionId, @RequestBody PollDayReportDTO dto) {
        boolean isCreated = reportService.saveOrUpdatePollDayReport(electionId, dto);
        return new ThedalResponse(ThedalSuccess.POLL_DAY_REPORT_UPDATED, HttpStatus.ACCEPTED);
       
    }

    @Operation(summary = "Get poll day reports", description = "Fetches all poll day reports for a specific election")
    @GetMapping("/pollday/reports/{electionId}")
    public ThedalResponse<List<PollDayReportDTO>> getPollDayReports(@PathVariable Long electionId) {
        List<PollDayReportDTO> reports = reportService.getPollDayReports(electionId);
        return new ThedalResponse<>(ThedalSuccess.POLL_DAY_REPORTS_FETCHED, reports);
    }
    
//    @Operation(summary = "Save or update election dashboard report", description = "Saves a new election dashboard report or updates an existing one for a specific election and user")
//    @PutMapping("/dashboard/save/{electionId}")
//    public ThedalResponse<Void> saveElectionDashboardReport(@PathVariable Long electionId, @RequestBody ElectionDashboardDTO dto) {
//        boolean isCreated = reportService.saveOrUpdateElectionDashboardReport(electionId, dto);
//        return new ThedalResponse<>(ThedalSuccess.DASHBOARD_REPORT_CREATED);
//    }
//
//    @Operation(summary = "Get election dashboard reports", description = "Fetches all election dashboard reports for a specific election and user")
//    @GetMapping("/dashboard/reports/{electionId}")
//    public ThedalResponse<List<ElectionDashboardDTO>> getElectionDashboardReports(
//            @PathVariable Long electionId,
//            @RequestParam Long userId) {
//        List<ElectionDashboardDTO> reports = reportService.getElectionDashboardReports(electionId, userId);
//        return new ThedalResponse<>(ThedalSuccess.DASHBOARD_REPORTS_FETCHED, reports);
//    }
    @Operation(summary = "Save or update election dashboard report", description = "Saves a new election dashboard report or updates an existing one for a specific election")
    @PutMapping("/dashboard/save/{electionId}")
    public ThedalResponse<Void> saveElectionDashboardReport(@PathVariable Long electionId, @RequestBody ElectionDashboardDTO dto) {
        boolean isCreated = reportService.saveOrUpdateElectionDashboardReport(electionId, dto);
        return new ThedalResponse<>(ThedalSuccess.DASHBOARD_REPORT_CREATED);
    }

    @Operation(summary = "Get election dashboard reports", description = "Fetches all election dashboard reports for a specific election, optionally filtered by booth number")
    @GetMapping("/dashboard/reports/{electionId}")
    public ThedalResponse<List<ElectionDashboardDTO>> getElectionDashboardReports(
            @PathVariable Long electionId,
            @RequestParam(required = false) String boothNumber) {
        List<ElectionDashboardDTO> reports = reportService.getElectionDashboardReports(electionId, boothNumber);
        return new ThedalResponse<>(ThedalSuccess.DASHBOARD_REPORTS_FETCHED, reports);
    }
    
//    @RestController
//    @RequestMapping("/api/health")
//    public class HealthCheckController {
//        
//        @Autowired
//        private RedisTemplate<String, String> redisTemplate;
//        
//        @GetMapping("/redis")
//        public ResponseEntity<String> checkRedis() {
//            try {
//                redisTemplate.opsForValue().set("health-check", "ok", 10, TimeUnit.SECONDS);
//                return ResponseEntity.ok("Redis connection is working");
//            } catch (Exception e) {
//                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                    .body("Redis connection failed: " + e.getMessage());
//            }
//        }
//    }

    
}
