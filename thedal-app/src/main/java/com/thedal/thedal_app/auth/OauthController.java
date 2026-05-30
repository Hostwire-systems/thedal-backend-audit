package com.thedal.thedal_app.auth;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.auth.dtos.CompleteSignupDto;
import com.thedal.thedal_app.auth.dtos.LoginResponseDto;
import com.thedal.thedal_app.util.Response;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/oauth")
public class OauthController {

    @Autowired
    AuthService userService;
    
	 @PutMapping("/complete-signup")
	    public ResponseEntity<Response<LoginResponseDto>> completeSignup(
	            @RequestParam String email,
	            @Valid @RequestBody CompleteSignupDto request) throws IOException {
	        return userService.completeSignup(email, request);
	    }
}