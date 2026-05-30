package com.thedal.thedal_app.cpanel;



import org.springframework.web.multipart.MultipartFile;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoterHistoryRequest {
    private String voterHistoryName;
    private MultipartFile voterHistoryImage;
}
