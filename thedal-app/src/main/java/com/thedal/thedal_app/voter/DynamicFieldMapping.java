package com.thedal.thedal_app.voter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dynamic_field_mapping")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DynamicFieldMapping {

    public DynamicFieldMapping(Long accountId2, Long electionId2, String fieldName2, String columnName2) {
		this.accountId=accountId2;
		this.electionId=electionId2;
		this.fieldName=fieldName2;
		this.columnName=columnName2;
	}

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "column_name", nullable = false)
    private String columnName;

}