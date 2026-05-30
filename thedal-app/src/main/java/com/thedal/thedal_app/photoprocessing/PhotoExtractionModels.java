package com.thedal.thedal_app.photoprocessing;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
class PhotoExtractionResponse {
    private String jobId;
    private boolean success;
    private List<ExtractedPhoto> photos;
    private PhotoExtractionMetadata metadata;
    private String message;
    private String error;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class ExtractedPhoto {
    @JsonProperty("serial_no")
    private Long serialNo;
    private String filename;
    private String filePath;
    private String dimensions;
    private String extractionMethod;
    private Double confidence;
    private Integer pageNumber;
    private PhotoPosition cardPosition;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class PhotoPosition {
    private Integer row;
    private Integer col;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class PhotoExtractionMetadata {
    private String jobId;
    private String partNo;
    private Long electionId;
    private String pdfFilename;
    private LocalDateTime extractionTimestamp;
    private Integer totalPhotosExtracted;
    private String extractionAccuracy;
    private ProcessingSummary processingSummary;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class ProcessingSummary {
    private Integer totalCards;
    private Integer photosFound;
    private Integer highConfidence;
    private Integer mediumConfidence;
    private Integer lowConfidence;
}
