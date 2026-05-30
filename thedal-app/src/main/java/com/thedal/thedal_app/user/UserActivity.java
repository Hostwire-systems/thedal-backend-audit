package com.thedal.thedal_app.user;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "user_activity")
public class UserActivity {

    @Id
    private String id; // userId-datetime format
    private Long userId;
    private LocalDateTime lastLogin;
    private int loginCount;

    public UserActivity(Long userId, LocalDateTime lastLogin, int loginCount) {
        this.id = userId + "-" + lastLogin.toString(); // Assuming you want to set the ID like this
        this.userId = userId;
        this.lastLogin = lastLogin;
        this.loginCount = loginCount;
    }

}
