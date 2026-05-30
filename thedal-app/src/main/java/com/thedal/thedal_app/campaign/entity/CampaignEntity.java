package com.thedal.thedal_app.campaign.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.campaign.dto.CampaignCreateRequest;
import com.thedal.thedal_app.campaign.dto.CampaignFilters;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "campaigns")
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignEntity {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "channel", nullable = false, length = 20)
    private String channel; // whatsapp | sms
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "sender_id", length = 100)
    private String senderId;
    
    @Column(name = "language", length = 10)
    private String language;
    
    @Lob
    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;
    
    @Lob
    @Column(name = "buttons_json", columnDefinition = "TEXT")
    private String buttonsJson; // JSON string of buttons
    
    @Lob
    @Column(name = "media_json", columnDefinition = "TEXT")
    private String mediaJson; // JSON string of media
    
    @Lob
    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson; // JSON string of tags
    
    @Lob
    @Column(name = "filters_json", columnDefinition = "TEXT")
    private String filtersJson; // JSON string of filters
    
    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT"; // DRAFT|SCHEDULED|SENDING|SENT|FAILED
    
    @Column(name = "recipients_count")
    private Long recipientsCount;
    
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    
    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;
    
    @CreationTimestamp
    @Column(name = "db_created_at")
    private LocalDateTime dbCreatedAt;
    
    @UpdateTimestamp
    @Column(name = "db_updated_at")
    private LocalDateTime dbUpdatedAt;
    
    // Helper methods for JSON serialization/deserialization
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public List<CampaignCreateRequest.Button> getButtons() {
        if (buttonsJson == null || buttonsJson.isEmpty()) return null;
        try {
            return objectMapper.readValue(buttonsJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, CampaignCreateRequest.Button.class));
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    public void setButtons(List<CampaignCreateRequest.Button> buttons) {
        if (buttons == null) {
            this.buttonsJson = null;
            return;
        }
        try {
            this.buttonsJson = objectMapper.writeValueAsString(buttons);
        } catch (JsonProcessingException e) {
            this.buttonsJson = null;
        }
    }
    
    public CampaignCreateRequest.Media getMedia() {
        if (mediaJson == null || mediaJson.isEmpty()) return null;
        try {
            return objectMapper.readValue(mediaJson, CampaignCreateRequest.Media.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    public void setMedia(CampaignCreateRequest.Media media) {
        if (media == null) {
            this.mediaJson = null;
            return;
        }
        try {
            this.mediaJson = objectMapper.writeValueAsString(media);
        } catch (JsonProcessingException e) {
            this.mediaJson = null;
        }
    }
    
    public List<String> getTags() {
        if (tagsJson == null || tagsJson.isEmpty()) return null;
        try {
            return objectMapper.readValue(tagsJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    public void setTags(List<String> tags) {
        if (tags == null) {
            this.tagsJson = null;
            return;
        }
        try {
            this.tagsJson = objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            this.tagsJson = null;
        }
    }
    
    public CampaignFilters getFilters() {
        if (filtersJson == null || filtersJson.isEmpty()) return null;
        try {
            return objectMapper.readValue(filtersJson, CampaignFilters.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    public void setFilters(CampaignFilters filters) {
        if (filters == null) {
            this.filtersJson = null;
            return;
        }
        try {
            this.filtersJson = objectMapper.writeValueAsString(filters);
        } catch (JsonProcessingException e) {
            this.filtersJson = null;
        }
    }
}