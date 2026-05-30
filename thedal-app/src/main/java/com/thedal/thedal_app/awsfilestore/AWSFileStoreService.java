package com.thedal.thedal_app.awsfilestore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Service wrapper for AWS file operations
 */
@Service
public class AWSFileStoreService {

    @Autowired
    private AwsFileUpload awsFileUpload;

    /**
     * Upload a file to S3 and return the URL
     */
    public String uploadFile(MultipartFile file, String s3Key) throws IOException {
        // Extract bucket name from application properties
        String bucket = "thedalnew"; // This should match your S3 bucket
        
        return awsFileUpload.uploadMultipartFile(file, s3Key, bucket);
    }
    
    /**
     * Upload a file to S3 with custom bucket
     */
    public String uploadFile(MultipartFile file, String s3Key, String bucket) throws IOException {
        return awsFileUpload.uploadMultipartFile(file, s3Key, bucket);
    }
}
