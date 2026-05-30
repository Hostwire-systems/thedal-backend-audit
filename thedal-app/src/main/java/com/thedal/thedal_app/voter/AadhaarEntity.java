package com.thedal.thedal_app.voter;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "aadhaar_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AadhaarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long accountId;
    private Long electionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> aadhaarData;
    
    @Column(name = "aadhaar_verified", nullable = false, columnDefinition = "boolean default false")
    private boolean aadhaarVerified;

//    @Column(name = "aadhaar_number")
//    private String aadhaarNumber;

}