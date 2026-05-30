package com.thedal.thedal_app.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {

    @NotBlank(message = "Email or mobile is required")
    private String user;

    @NotBlank(message = "Password is required")
    private String password;
}
