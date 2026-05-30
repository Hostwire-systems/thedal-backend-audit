package com.thedal.thedal_app.files;

import com.thedal.thedal_app.voter.BulkUploadEntity;
import com.thedal.thedal_app.voter.BulkUploadMemberEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Table(name = "file_en")
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Files {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Enumerated(EnumType.STRING)
	private HandlerType handlerType;
	
	private Long handlerFileId;
	
	private String fileName;
	
	private String url;
	
//	@Column(name = "bulk_upload_id")
//	private Long bulkUploadId;
	@OneToOne
    @JoinColumn(name = "bulk_upload_id") 
    private BulkUploadEntity bulkUpload; 
	
	@OneToOne
    @JoinColumn(name = "bulk_upload_member_id")
    private BulkUploadMemberEntity bulkUploadMember;
	
	private Integer orderIndex;
	private Boolean whatsappForward = false;
	
    private Boolean isActive = true;
 	
	public Files(HandlerType design, Long designFileId, String fileName, String url) {
		this.handlerType = design;
		this.handlerFileId = designFileId;
		this.fileName = fileName;
		this.url = url;
		this.isActive = true;
	}
	
	
}
