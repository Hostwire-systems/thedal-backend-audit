package com.thedal.thedal_app.voter.dto;

import java.util.ArrayList;
import java.util.List;

//@Setter
//@Getter
public class BulkUploadResponse {

	private Long bulkUploadId;
	private List<String> validationErrors = new ArrayList<>();

    public BulkUploadResponse(Long bulkUploadId) {
        this.bulkUploadId = bulkUploadId;
    }

    public Long getBulkUploadId() {
        return bulkUploadId;
    }

    public void setBulkUploadId(Long bulkUploadId) {
        this.bulkUploadId = bulkUploadId;
    }
 
}
