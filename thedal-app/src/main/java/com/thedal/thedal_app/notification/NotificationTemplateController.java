package com.thedal.thedal_app.notification;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

@RestController
public class NotificationTemplateController {
    @Autowired
	private NotificationTemplateService notificationTemplateService;

	@PostMapping("/admin/load-notification-templates")
	// @PreAuthorize("hasRole('ROLE_ADMIN')")
	public void loadNotificationTemplates() throws JsonParseException, JsonMappingException, IOException {
		notificationTemplateService.uploadNotificationTemplates();
	}

}
