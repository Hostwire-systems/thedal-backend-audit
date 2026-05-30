package com.thedal.thedal_app.election.dtos;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DynamicFieldDTO {

	@JsonIgnore
    @JsonProperty("id")
    private Long id;

    @NotBlank(message = "Label is mandatory")
    @JsonProperty("label")
    private String label;
    
    //@NotBlank(message = "Name is mandatory")
    @JsonProperty("name")
    private String name;

    @NotBlank(message = "Type is mandatory")
    @JsonProperty("type")
    private String type;

    @NotNull(message = "Required field is mandatory")
    @JsonProperty("required")
    private Boolean required;

    @JsonProperty("options")
    private List<String> options;

    @JsonProperty("orderIndex")
    private Integer orderIndex;

    @JsonProperty("createdTime")
    private LocalDateTime createdTime;

    @JsonProperty("modifiedTime")
    private LocalDateTime modifiedTime;

    @JsonIgnore
    @JsonProperty("electionId")
    private Long electionId;
    
    @JsonProperty("status")
    private Boolean status;
    
    public DynamicFieldDTO(Long id, String label, String name, String type, Boolean required, Boolean status,
            List<String> options, Integer orderIndex, LocalDateTime createdTime,
            LocalDateTime modifiedTime, Long electionId) {
        this.id = id;
        this.label = label;
        this.name = name;
        this.type = type;
        this.required = required;
        this.status = status;
        this.options = options;
        this.orderIndex = orderIndex;
        this.createdTime = createdTime;
        this.modifiedTime = modifiedTime;
        this.electionId = electionId;
}
    
    
    
}