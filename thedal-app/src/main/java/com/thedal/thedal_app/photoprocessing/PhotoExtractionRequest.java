package com.thedal.thedal_app.photoprocessing;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhotoExtractionRequest {
    private String jobId;
    private String partNo;
    private Long electionId;
    private String filename;
    private byte[] pdfData;
    private LocalDateTime requestTime;
    
    // Page range parameters
    private Integer startPage;
    private Integer endPage;
    
    // Request metadata
    private String requestedBy;
    private String clientIp;
    private Long accountId;
}
