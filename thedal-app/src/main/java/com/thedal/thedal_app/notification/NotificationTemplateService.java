package com.thedal.thedal_app.notification;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.awsfilestore.AwsFileUpload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class NotificationTemplateService {

    @Autowired
    private AwsFileUpload awsFileUpload; // Use the AwsFileUpload class

    @Value("${aws.s3.notification.bucket}")
    private String s3Bucket;

    @Value("${aws.s3.notification.folder}")
    private String notificationFolder;

    public void uploadNotificationTemplates() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ClassPathResource classPathResource = new ClassPathResource("private/notificationTemplates.json");

        // Read JSON file into a Map<String, NotificationType>
        Map<String, NotificationType> templates = objectMapper.readValue(classPathResource.getInputStream(),
                new TypeReference<Map<String, NotificationType>>() {});

        // Upload each template to S3 in the specified folder
        for (Map.Entry<String, NotificationType> entry : templates.entrySet()) {
            // Convert the NotificationType to JSON
            String jsonTemplate = objectMapper.writeValueAsString(entry.getValue());

            // Set the object key with folder structure (folder + template name)
            String objectKey = notificationFolder + "/" + entry.getKey() + ".json";

            System.out.println("Uploading to bucket: " + s3Bucket + ", key: " + objectKey);

            // Create an InputStream from JSON string
            ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonTemplate.getBytes());

            // Use the AwsFileUpload to upload the template JSON to S3
            awsFileUpload.uploadTemplateFile(inputStream, objectKey, s3Bucket);
        }
    }
}
