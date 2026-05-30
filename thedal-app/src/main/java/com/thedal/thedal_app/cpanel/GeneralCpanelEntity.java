package com.thedal.thedal_app.cpanel;

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
@Table(name = "cpanel_general")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GeneralCpanelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment ID
    private Long id;

    @Column(name = "cpanel_name", nullable = false)
    private String cpanelName;

    //cpanel_value that can store a full text/string document content
    @Column(name = "cpanel_value", nullable = false, columnDefinition = "TEXT")
    private String cpanelValue;

}
