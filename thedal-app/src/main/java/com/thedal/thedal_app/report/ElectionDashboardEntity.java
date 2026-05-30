package com.thedal.thedal_app.report;

import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class ElectionDashboardEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    //private Long userId;
    private Long electionId;
    private String boothNumber;
    private String activeTab;
    @ElementCollection
    private List<String> demographics;
    @ElementCollection
    private List<String> issues;
    @ElementCollection
    private List<String> religion;
    @ElementCollection
    private List<String> caste;
    @ElementCollection
    private List<String> subcaste;
    @ElementCollection
    private List<String> languages;
    @ElementCollection
    private List<String> party;
    @ElementCollection
    private List<String> scheme;
    @ElementCollection
    private List<String> history;
    @ElementCollection
    private List<String> availability;
}