package com.thedal.thedal_app.awsfilestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.util.RandomTokenGenerator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ImageUpload {

    @Value("${aws.s3.image.bucket}")
    private String s3Bucket;

    @Autowired
    private AwsFileUpload awsFileUpload;

    public String uploadProfileImageToAWS(MultipartFile multipartFile) {
        return uploadImageToAWS("profile_", multipartFile);
    }

    public String uploadElectionImageToAWS(MultipartFile multipartFile) {
        return uploadImageToAWS("election_", multipartFile);
    }
    
    private String uploadImageToAWS(String fileNamePrefix, MultipartFile multipartFile) {

        String contentType = multipartFile.getContentType();
        if (!(MediaType.IMAGE_JPEG_VALUE.equals(contentType) ||
                MediaType.IMAGE_PNG_VALUE.equals(contentType))) {
            throw new IllegalArgumentException("Only JPEG or PNG files are supported");
        }

        long maxFileSize = 5 * 1024 * 1024; // 5MB
        if (multipartFile.getSize() > maxFileSize) {
            throw new IllegalArgumentException("Maximum allowed file size: 5MB");
        }

        String fileExtension = "." + awsFileUpload.getFileExtension(multipartFile.getOriginalFilename());
        String profilePictureName = fileNamePrefix + System.currentTimeMillis() + "_" + RandomTokenGenerator.generateToken(10) + fileExtension;
        log.info("Profile Picture Name: " + profilePictureName);

        // Create a temporary file
        File tempFile;
        try {
            tempFile = File.createTempFile("temp", fileExtension);
            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                fileOutputStream.write(multipartFile.getBytes());
            }
        } catch (IOException e) {
            log.error("Error creating temp file for upload", e);
            throw new IllegalArgumentException("IO exception while creating temp file");
        }

        String awsUrl = awsFileUpload.uploadToAWS(tempFile, profilePictureName, s3Bucket);

        if (!tempFile.delete()) {
            log.warn("Temporary file deletion failed: {}", tempFile.getName());
        }
        return awsUrl;
    }
}
