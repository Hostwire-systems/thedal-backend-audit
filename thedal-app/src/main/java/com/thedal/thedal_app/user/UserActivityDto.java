package com.thedal.thedal_app.user;
import java.time.LocalDateTime;

public class UserActivityDto {

    private LocalDateTime lastLogin;
    private int loginCount;

    // Default constructor for Jackson
    public UserActivityDto() {}

    // Constructor with parameters
    public UserActivityDto(LocalDateTime lastLogin, int loginCount) {
        this.lastLogin = lastLogin;
        this.loginCount = loginCount;
    }

    // Getters and setters
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public int getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }

}
