package com.thedal.thedal_app.cpanel;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "catalogue_items",
       indexes = {
           @Index(name = "idx_catalogue_name", columnList = "name")
       })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CatalogueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double price;

//    @Column(nullable = false)
//    private String description;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;


    @Column
    private String image;

    @Column
    private Long accountId;
    
    @Column(name = "order_index")
    private Integer orderIndex;
    
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean active = true;

    @CreatedDate
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}