package com.thedal.thedal_app.subscription;

import java.time.LocalDateTime;

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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscription_module")
public class SubscriptionModule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "module_key", unique = true, nullable = false, length = 100)
    private String moduleKey;
    
    @Column(name = "module_name", nullable = false, length = 200)
    private String moduleName;
    
    @Column(name = "module_description", length = 500)
    private String moduleDescription;
    
    @Column(name = "parent_module_id")
    private Long parentModuleId;
    
    @Column(name = "display_order")
    private Integer displayOrder;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "icon_name", length = 100)
    private String iconName;
    
    @Column(name = "route_path", length = 200)
    private String routePath;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
