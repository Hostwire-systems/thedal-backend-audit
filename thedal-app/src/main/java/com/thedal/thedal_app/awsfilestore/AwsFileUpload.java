package com.thedal.thedal_app.awsfilestore;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;
// import org.springframework.web.multipart.MultipartFile;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
//import jakarta.xml.bind.DatatypeConverter;
import com.thedal.thedal_app.util.RandomTokenGenerator;

import jakarta.xml.bind.DatatypeConverter;

@Component
public class AwsFileUpload {

    private String imageContentType = "image/jpeg";
    private long maxImageSize;
    private long minImageSize;
    private String s3AccessKey;
    private String s3SecretKey;
    private String s3Endpoint;
    private String s3Region;
    // Add this field to store the S3 client
    private AmazonS3 s3Client;
    
    private static final Logger log = LoggerFactory.getLogger(AwsFileUpload.class);

    @Autowired
    public AwsFileUpload(@Value("${aws.s3.accessKey}") String s3AccessKey,
            @Value("${aws.s3.secretKey}") String s3SecretKey,
            @Value("${aws.s3.endpoint}") String s3Endpoint,
            @Value("${aws.s3.region}") String s3Region,
            @Value("${files.image.size.max}") long maxImageSize,
            @Value("${files.image.size.min}") long minImageSize) {

        this.s3AccessKey = s3AccessKey;
        this.s3SecretKey = s3SecretKey;
        this.s3Endpoint = s3Endpoint;
        this.s3Region = s3Region;
        this.maxImageSize = maxImageSize;
        this.minImageSize = minImageSize;
        
        
     // Initialize S3 client in constructor
        AWSCredentialsProvider spacesCreds = new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(s3AccessKey, s3SecretKey));
    this.s3Client = AmazonS3ClientBuilder.standard()
          .withCredentials(spacesCreds)
          .withEndpointConfiguration(
              new AwsClientBuilder.EndpointConfiguration(s3Endpoint, s3Region))
          .build();
        
    }
    
    


    // private AwsFileUpload(S3CredsConfig s3CredsConfig) {
    // initialize(s3CredsConfig);
    // }
    // Factory method to create an instance of FileUploadUtil
    // public static AwsFileUpload createInstance(S3CredsConfig s3CredsConfig) {
    // return new AwsFileUpload(s3CredsConfig);
    // }
    // private static void initialize(S3CredsConfig s3CredsConfig) {
    // s3AccessKey = s3CredsConfig.getS3AccessKey();
    // s3SecretKey = s3CredsConfig.getS3SecretKey();
    // s3Endpoint = s3CredsConfig.getS3Endpoint();
    // s3Region = s3CredsConfig.getS3Region();
    // maxImageSize = s3CredsConfig.getMaxImageSize();
    // minImageSize = s3CredsConfig.getMinImageSize();
    // }
    public String uploadMultipartFile(MultipartFile file, String fileName, String s3Bucket) throws IOException {
        File convFile = convertToFile(file);
        String imageURL = uploadToAWS(convFile, fileName, s3Bucket);
        convFile.delete();

        return imageURL;
    }

    public String uploadTemplateFile(InputStream inputStream, String fileName, String s3Bucket) throws IOException {
        File tempFile = File.createTempFile("template_", ".json"); // Temporary file to hold data

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        // Upload the temp file to S3
        String templateURL = uploadToAWS(tempFile, fileName, s3Bucket);
        tempFile.delete(); // Clean up

        return templateURL;
    }

    public void validateImage(MultipartFile image) {
        if (image.isEmpty() || image.getSize() > maxImageSize || image.getSize() < minImageSize)
            throw new IllegalArgumentException("Image size should be between 50kb and 5mb");
        if (!imageContentType.equals(image.getContentType()))
            throw new IllegalArgumentException("Image should be in jpeg format");
    }

    private File convertToFile(MultipartFile image) throws IOException {
        String fileExtension = getFileExtension(image.getOriginalFilename());
        String safeFileName = RandomTokenGenerator.generateToken(10) + System.currentTimeMillis() + "." + fileExtension;
        // Use a secure directory (application-specific temp folder, for instance)
        File secureTempDir = new File(System.getProperty("java.io.tmpdir"));
        if (!secureTempDir.exists()) {
            secureTempDir.mkdirs(); // Ensure directory exists
            secureTempDir.setWritable(true, true); // Restrict writable access to owner only
        }
        // File convFile = new File(safeFileName);
        File convFile = File.createTempFile(safeFileName, "." + fileExtension, secureTempDir);
        convFile.setReadable(true, true);
        convFile.setWritable(true, true);
        // convFile.createNewFile();
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(image.getBytes());
        }
        return convFile;
    }

    public String getFileExtension(String fileName) {
        // Get the file extension in a safe way
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        } else {
            return ""; // Default if no extension is found
        }
    }
    // public static String uploadToAWS(File file, String fileName, String s3Bucket)
    // {
    // AWSCredentialsProvider spacesCreds = new AWSStaticCredentialsProvider(new
    // BasicAWSCredentials(
    // s3AccessKey, s3SecretKey));
    // AmazonS3 spacesClient =
    // AmazonS3ClientBuilder.standard().withCredentials(spacesCreds)
    // .withEndpointConfiguration(new
    // AwsClientBuilder.EndpointConfiguration(s3Endpoint, s3Region))
    // .build();
    // // "CannedAccessControlList.PublicRead" makes readable for everyone
    // spacesClient.putObject(new PutObjectRequest(s3Bucket, fileName, file)
    // .withCannedAcl(CannedAccessControlList.PublicRead));
    // return spacesClient.getUrl(s3Bucket, fileName).toString();
    // }

    public String uploadToAWS(File file, String fileName, String s3Bucket) {
        log.info("S3_UPLOAD: Starting upload - file: {}, fileName: {}, bucket: {}", 
                file.getAbsolutePath(), fileName, s3Bucket);
        log.info("S3_UPLOAD: File exists: {}, file size: {} bytes", file.exists(), file.length());
        log.info("S3_UPLOAD: S3 config - endpoint: {}, region: {}, accessKey present: {}", 
                s3Endpoint, s3Region, (s3AccessKey != null && !s3AccessKey.isEmpty()));
        
        try {
            AWSCredentialsProvider spacesCreds = new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                    s3AccessKey, s3SecretKey));
            log.info("S3_UPLOAD: Created AWS credentials provider for file: {}", fileName);
            
            AmazonS3 spacesClient = AmazonS3ClientBuilder.standard()
                    .withCredentials(spacesCreds)
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, s3Region))
                    .build();
            log.info("S3_UPLOAD: Created S3 client for file: {}", fileName);

            // Create put request
            PutObjectRequest putRequest = new PutObjectRequest(s3Bucket, fileName, file);
            log.info("S3_UPLOAD: Created put object request for file: {}", fileName);
            
            // Upload file
            log.info("S3_UPLOAD: Starting putObject operation for file: {}", fileName);
            PutObjectResult putResult = spacesClient.putObject(putRequest);
            log.info("S3_UPLOAD: PutObject completed successfully for file: {}, ETag: {}", 
                    fileName, putResult.getETag());

            // Get URL
            log.info("S3_UPLOAD: Getting URL for uploaded file: {}", fileName);
            String url = spacesClient.getUrl(s3Bucket, fileName).toString();
            log.info("S3_UPLOAD: Successfully completed upload for file: {}, URL: {}", fileName, url);
            
            return url;
            
        } catch (Exception e) {
            log.error("S3_UPLOAD: Failed to upload file: {}, error: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("S3 upload failed: " + e.getMessage(), e);
        }
    }

    public void deleteS3Object(String s3Bucket, String s3ObjectKey) {
        AWSCredentialsProvider spacesCreds = new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                s3AccessKey, s3SecretKey));
        AmazonS3 spacesClient = AmazonS3ClientBuilder.standard()
                .withCredentials(spacesCreds)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, s3Region))
                .build();
        // Delete the object
        spacesClient.deleteObject(s3Bucket, s3ObjectKey);
    }

    public S3Object getS3Object(String s3Bucket, String s3ObjectKey) {
        AWSCredentialsProvider spacesCreds = new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                s3AccessKey, s3SecretKey));
        AmazonS3 spacesClient = AmazonS3ClientBuilder.standard()
                .withCredentials(spacesCreds)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, s3Region))
                .build();
        return spacesClient.getObject(s3Bucket, s3ObjectKey);
    }

    public static String getKeyFromUrl(String url) {
        if (StringUtils.isBlank(url))
            return null;
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }

    public File base64ToFileConverter(String base64File, String fileName)
            throws FileNotFoundException, IOException {
        byte[] decodedBytes = DatatypeConverter.parseBase64Binary(base64File);
        File outputFile = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(decodedBytes);
        }
        return outputFile;
    }

    // new one uploadToAWS for voter
    public String uploadToAWSForvoter(File file, String fileName, String s3Bucket) {
        AWSCredentialsProvider awsCreds = new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                s3AccessKey, s3SecretKey));
        
        // Use the standard AWS S3 client builder without custom endpoint
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(awsCreds)
                .withRegion(Regions.fromName(s3Region))  // Use the region directly
                .build();
    
        s3Client.putObject(new PutObjectRequest(s3Bucket, fileName, file));
        return s3Client.getUrl(s3Bucket, fileName).toString();
    }

    // public void voterlatandlongupload(String key, byte[] data) throws IOException {
    //     File tempFile = File.createTempFile("voter_json_", ".json");
    //     try (FileOutputStream fos = new FileOutputStream(tempFile)) {
    //         fos.write(data);
    //     }

    //     // Correct bucket name
    //     String bucketName = "thedalnew";

    //     // Path structure that matches your S3 configuration
    //     // Add thedalnew prefix to match your actual structure
    //     String s3Key = "thedalnew/voter-location/" + key;
    //     // Alternatively, you might need:
    //     // String s3Key = "voter-location/" + key;

    //     // Upload to correct bucket
    //     uploadToAWSForvoter(tempFile, s3Key, bucketName);

    //     // Delete the temp file after upload
    //     tempFile.delete();
    // }
    
    public void voterlatandlongupload(String key, File jsonFile) throws IOException {
        // Correct bucket name
        String bucketName = "thedalnew";
    
        // Path structure that matches your S3 configuration
        String s3Key = "thedalnew/voter-location/" + key;
    
        // Upload to correct bucket using the file directly (more memory efficient)
        uploadToAWSForvoter(jsonFile, s3Key, bucketName);
    
        // No need to delete the file here, as it should be managed by the calling method
    }
    
    // Keep the original method for backward compatibility
    public void voterlatandlongupload(String key, byte[] data) throws IOException {
        File tempFile = File.createTempFile("voter_json_", ".json");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(data);
        }
    
        // Use the file-based upload method
        voterlatandlongupload(key, tempFile);
    
        // Delete the temp file after upload
        tempFile.delete();
    }
    
    /**
     * Upload byte array directly to S3 without creating temp file
     * Optimized for bulk photo uploads
     */
    public String uploadBytesToAWS(byte[] fileContent, String fileName, String s3Bucket, String contentType) {
        log.debug("S3_UPLOAD_BYTES: Uploading {} bytes to {}/{}", fileContent.length, s3Bucket, fileName);
        
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileContent.length);
            metadata.setContentType(contentType);
            
            try (ByteArrayInputStream bis = new ByteArrayInputStream(fileContent)) {
                // Note: No ACL set - bucket doesn't allow ACLs
                PutObjectRequest putRequest = new PutObjectRequest(s3Bucket, fileName, bis, metadata);
                
                s3Client.putObject(putRequest);
            }
            
            String url = s3Client.getUrl(s3Bucket, fileName).toString();
            log.debug("S3_UPLOAD_BYTES: Successfully uploaded to {}", url);
            return url;
            
        } catch (Exception e) {
            log.error("S3_UPLOAD_BYTES: Failed to upload {}: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to upload to S3: " + e.getMessage(), e);
        }
    }
    
    // public boolean doesObjectExist(String bucketName, String key) {
    //     AmazonS3 s3Client = getS3Client();
        
    //     return s3Client.doesObjectExist(bucketName, key);
    // }

    public boolean doesObjectExist(String bucketName, String key) {
        AmazonS3 s3Client = getS3Client();
    
        log.info("Checking existence of S3 object - Bucket: {}, Key: {}", bucketName, key);
    
        boolean exists = s3Client.doesObjectExist(bucketName, key);
    
        if (exists) {
            log.info("S3 object FOUND - Bucket: {}, Key: {}", bucketName, key);
        } else {
            log.warn("S3 object NOT FOUND - Bucket: {}, Key: {}", bucketName, key);
        }
    
        return exists;
    }

    
    

    public String generatePresignedUrl(String bucketName, String key, int expirationInSeconds) {
        AmazonS3 s3Client = getS3Client();
        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = expiration.getTime() + 1000L * expirationInSeconds;
        expiration.setTime(expTimeMillis);
        return s3Client.generatePresignedUrl(bucketName, key, expiration).toString();
    }

    private AmazonS3 getS3Client() {
        AWSCredentialsProvider creds = new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(s3AccessKey, s3SecretKey)
        );
        return AmazonS3ClientBuilder.standard()
                .withCredentials(creds)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, s3Region))
                .build();
    }
    
    public String uploadToAWS(InputStream inputStream, String fileKey, String bucket) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            
            PutObjectRequest request = new PutObjectRequest(bucket, fileKey, inputStream, metadata);
            s3Client.putObject(request);
            
            return s3Client.getUrl(bucket, fileKey).toString();
        } catch (AmazonServiceException e) {
            throw new RuntimeException("AWS S3 upload failed: " + e.getMessage(), e);
        }
    }


}