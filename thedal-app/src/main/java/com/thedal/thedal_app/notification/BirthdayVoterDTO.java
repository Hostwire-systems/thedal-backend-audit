package com.thedal.thedal_app.notification;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BirthdayVoterDTO {
    private String epicNumber;
    private String mobileNo;
    private String voterFnameEn;
    private String voterLnameEn;
    // Label indicating if this birthday is today or tomorrow (currently only TODAY supported by endpoint)
    private String dateLabel;
}