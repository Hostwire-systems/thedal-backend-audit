package com.thedal.thedal_app.notification;



//import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class NotificationType {

    
	private String id;
    
    private String subject;
    
    private String body;
    
    private String url;
    
    private String message;
}
