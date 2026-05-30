package com.thedal.thedal_app.settings.electionsettings.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LanguageResponseDTO {

    private Long id;
    
    private String languageName;

    @JsonIgnore
    private Long accountId;
    @JsonIgnore
    private Long electionId;
    private String state;
    
    private Integer orderIndex;
    @JsonIgnore
    public LanguageResponseDTO(){
    }

    public LanguageResponseDTO(Long id, String languageName, Long accountId, Long electionId, Integer orderIndex, String state) {
        this.id = id;
        this.languageName = languageName;
        this.accountId = accountId;
        this.electionId = electionId;
        this.orderIndex = orderIndex;
        this.state = state;
    }

    public LanguageResponseDTO(Long id,String languageName,String state){
        this.id=id;
        this.languageName=languageName;
        this.state=state;
    }
    
}
