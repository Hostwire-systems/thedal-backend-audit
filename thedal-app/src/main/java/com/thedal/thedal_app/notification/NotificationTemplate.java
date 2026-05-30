package com.thedal.thedal_app.notification;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.notification.NotificationServiceImpl;
import com.thedal.thedal_app.notification.NotificationType;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.election.ElectionEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;

@Component
public class NotificationTemplate {
     private static final Logger LOGGER= LogManager.getLogger(NotificationServiceImpl.class);

    // @Autowired
    // private AmazonS3 s3Client;

    // @Value("${aws.s3.bucket}")
    // private String s3Bucket;

    @Value("${aws.s3.notification.bucket}")
    private String s3Bucket;

    @Autowired
    private AwsFileUpload awsFileUpload;

    @Value("${aws.s3.notification.folder}")
    private String notificationFolder;

    /**
     * Fetches a NotificationType template from S3 by template ID.
     *
     * @param templateId The ID of the template to fetch.
     * @return The NotificationType object corresponding to the template ID.
     */
    

     public NotificationType getTemplateById(String templateId) {
        // Construct the object key for the S3 bucket
        String objectKey = notificationFolder + "/" + templateId + ".json";
    
        try {
            // Retrieve the object from S3
            S3Object s3Object = awsFileUpload.getS3Object(s3Bucket, objectKey);
            
            // Read the content of the object and map it to NotificationType
            ObjectMapper objectMapper = new ObjectMapper();
            NotificationType notificationTemplate = objectMapper.readValue(s3Object.getObjectContent(), NotificationType.class);
            LOGGER.info("insidegetTemplateByid:notificationType:{}",notificationTemplate);
            return notificationTemplate;
        } catch (IOException e) {
            // Handle exceptions like IOException, StreamReadException, DatabindException
            e.printStackTrace();
            // You can return null or throw a custom exception here
            return null;
        }
    }
    

    /**
     * Retrieves the registration success template and sets the user's name.
     *
     * @param election The ElectionEntity to get the user's details.
     * @return The modified NotificationType object.
     */
    // public NotificationType registrationSuccessSetValues(ElectionEntity election) {
    //     // Fetch the template by ID
    //     NotificationType template = getTemplateById("registrationSuccess");
        
    //     // Check if the template is null
    //     if (template == null) {
    //         throw new ThedalException(ThedalError.NOTIFICATION_TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND);
    //     }

    //     LOGGER.info("Template body before replacement: {}", template.getBody());

    //     // Replace placeholders in the template body with actual values
    //     template.setBody(template.getBody().replace("{username}", election.getElectionName()));
    //     LOGGER.info("Template body before replacement: {}", template.getBody());

    //     return template;
    // }

    public NotificationType deleteElection(ElectionEntity election) {
        // Fetch the template by ID
        NotificationType template = getTemplateById("deleteElection");
        
        // Check if the template is null
        if (template == null) {
            throw new ThedalException(ThedalError.NOTIFICATION_TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        LOGGER.info("Template body before replacement: {}", template.getBody());

        // Replace placeholders in the template body with actual values
        template.setBody(template.getBody().replace("{userId}", String.valueOf(election.getId())));
        LOGGER.info("Template body before replacement: {}", template.getBody());

        return template;
    }


    public NotificationType bulkUploadStarted(String fileName, Long electionId, Long bulkUploadId) {
        NotificationType template = getTemplateById("bulkUploadStarted");
        if (template == null) {
            throw new ThedalException(ThedalError.NOTIFICATION_TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        template.setBody(template.getBody()
            .replace("{electionId}", String.valueOf(electionId))
            .replace("{fileName}", fileName)
            .replace("{uploadId}", String.valueOf(bulkUploadId)));

        return template;
    }

    public NotificationType bulkUploadCompleted(String fileName, Long electionId, Long bulkUploadId) {
        NotificationType template = getTemplateById("bulkUploadCompleted");
        if (template == null) {
            throw new ThedalException(ThedalError.NOTIFICATION_TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        template.setBody(template.getBody()
            .replace("{electionId}", String.valueOf(electionId))
            .replace("{fileName}", fileName)
            .replace("{uploadId}", String.valueOf(bulkUploadId)));

        return template;
    }

    public NotificationType bulkUploadFailed(String fileName, Long electionId, Long bulkUploadId) {
        NotificationType template = getTemplateById("bulkUploadFailed");
        if (template == null) {
            throw new ThedalException(ThedalError.NOTIFICATION_TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
    
        template.setBody(template.getBody()
            .replace("{electionId}", String.valueOf(electionId))
            .replace("{fileName}", fileName)
            .replace("{uploadId}", String.valueOf(bulkUploadId)));
    
        return template;
    }
    
    // public NotificationType update(ElectionEntity election) {
    //     // Fetch the template by ID
    //     NotificationType template = getTemplateById("update");
        
    //     // Check if the template is null
    //     if (template == null) {
    //         throw new ThedalException(ThedalError.NOTIFICATION_TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND);
    //     }

    //     LOGGER.info("Template body before replacement: {}", template.getBody());

    //     // Replace placeholders in the template body with actual values
    //     template.setBody(template.getBody().replace("{userId}",String.valueOf(election.getId())));
    //     LOGGER.info("Template body before replacement: {}", template.getBody());


    //     return template;
    // }



}