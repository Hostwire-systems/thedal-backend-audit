package com.thedal.thedal_app.volunteer;

import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.volunteer.dto.VolunteerDetailsDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerActivityResponseDTO;
import com.thedal.thedal_app.volunteer.dto.VolunteerLocationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/mongo/volunteers")
public class MongoVolunteerController {
    @Autowired
    private MongoVolunteerService mongoVolunteerService;

    // 1. Get Volunteer by User and Election (match Postgres URL structure)
    @GetMapping("/election/{electionId}/user/{userId}")
    public ThedalResponse<VolunteerDetailsDTO> getVolunteerDetails(
            @PathVariable("electionId") Long electionId,
            @PathVariable("userId") Long userId) {
        return mongoVolunteerService.getVolunteerByUserId(userId, electionId);
    }

    // 2. Get All Volunteers with Location Data (match Postgres URL structure)
    @GetMapping("/all/election/{electionId}/user/{userId}")
    public ThedalResponse<Page<VolunteerLocationDto>> getAllVolunteersWithLocationData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable Long electionId,
            @PathVariable Long userId) {
        return mongoVolunteerService.getAllVolunteersWithLocationData(page, size, userId, electionId);
    }

    // 3. Get Volunteer Activity (match Postgres URL structure)
    @GetMapping("/activity/{userId}/{electionId}")
    public ThedalResponse<Page<VolunteerActivityResponseDTO>> trackVolunteerActivitiesLocation(
            @PathVariable("userId") Long userId,
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "startDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return mongoVolunteerService.getVolunteerActivity(userId, electionId, startDate, endDate, page, size);
    }

    // 4. Filter by Booths and Mobile (match Postgres URL structure)
    @GetMapping("/election/{electionId}/by-booth-and-mobile-and-user")
    public ThedalResponse<List<VolunteerDetailsDTO>> getVolunteerByAssignedBoothsAndMobileNumber(
            @PathVariable Long electionId,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String assignedBooths,
            @RequestParam(required = false) Long userId) {
        List<Long> assignedBoothList = null;
        if (assignedBooths != null && !assignedBooths.isEmpty()) {
            assignedBoothList = Arrays.stream(assignedBooths.split(","))
                    .map(String::trim)
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        }
        return mongoVolunteerService.getVolunteerByAssignedBoothsAndMobileNumber(electionId, assignedBoothList, mobileNumber, userId);
    }

    // 6. Check-in status endpoint (mirroring Postgres)
    @GetMapping("/{userId}/{electionId}/check-in/status")
    public ThedalResponse<Boolean> isCheckedIn(@PathVariable("userId") Long userId, @PathVariable("electionId") Long electionId) {
        return mongoVolunteerService.isCheckedIn(userId, electionId);
    }
}
