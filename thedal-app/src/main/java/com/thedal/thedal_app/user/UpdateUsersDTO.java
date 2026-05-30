package com.thedal.thedal_app.user;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUsersDTO {
        private String firstName;
        private String lastName;
        private String role;
        private String email;
        private String mobileNumber;
        private Boolean slipBox;
        private LocalDateTime expiryAt;

        

}
