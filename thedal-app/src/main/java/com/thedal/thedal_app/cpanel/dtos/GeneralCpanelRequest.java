package com.thedal.thedal_app.cpanel.dtos;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GeneralCpanelRequest {

    private String termsAndConditions;
    private String privacyPolicy;
    private String faq;
    private String about;

}
