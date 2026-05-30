package com.thedal.thedal_app.election;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
	    name = "templates",
	    uniqueConstraints = {
	    		@UniqueConstraint(columnNames = {"slip_id"}),
	        //@UniqueConstraint(columnNames = {"template_id", "account_id", "election_id"}),
	        @UniqueConstraint(columnNames = {"template_name", "account_id", "election_id"}) 
	    }
	)
public class TemplateEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "slip_id", nullable = false, unique = true) 
    private String slipId; 
    
    //@Column(name = "template_id", nullable = false, unique = true)
    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    //@Column(name = "image_url", nullable = false)
    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    @Column(name = "image_status", nullable = true)
    private Boolean imageStatus;
    
    private Integer orderIndex;
    
    @Column(name = "voter_slip_header")
    private String voterSlipHeader;
    @Column(name = "candidate_info_image_footer")
    private String candidateInfoImageFooter;


}
