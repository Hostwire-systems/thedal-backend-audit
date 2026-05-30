package com.thedal.thedal_app.settings.electionsettings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LanguageResponse {
    private Long id;
    
    private String languageName;
    private Integer orderIndex;
    private String state;

    
    public LanguageResponse(){
    }

    public LanguageResponse(Long id,String languageName, Integer orderIndex, String state){
        this.id=id;
        this.languageName=languageName;
        this.orderIndex=orderIndex;
        this.state=state;
    }
}
