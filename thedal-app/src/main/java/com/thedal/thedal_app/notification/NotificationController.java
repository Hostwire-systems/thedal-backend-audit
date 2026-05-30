package com.thedal.thedal_app.notification;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {

    @Autowired
    private NotificationServiceImpl notificationService;
    
    @Autowired
    private NotificationsLogRepository notificationRepository;
    
    @GetMapping
    public ResponseEntity<List<NotificationsEntity>> getLast30DaysNotifications(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
    	List<NotificationsEntity> notificationLogs= notificationService.getLast30DaysNotifications(page, size);
    	return ResponseEntity.ok(notificationLogs);
    }

    @DeleteMapping
    public void deleteOldNotifications() {
        notificationService.deleteOldNotifications();
    }
    
    @GetMapping("/birthday-voters/{electionId}")
    public ResponseEntity<List<BirthdayVoterDTO>> getTodaysBirthdayVoters(
            @PathVariable Long electionId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        log.info("Fetching today's birthday voters for electionId={}, page={}, size={}", electionId, page, size);
        List<BirthdayVoterDTO> voterDTOs = notificationService.getVotersWithBirthdayToday(electionId, page, size);
        log.info("Returning {} birthday voters for electionId={}", voterDTOs.size(), electionId);
        return ResponseEntity.ok(voterDTOs);
    }
    
}


