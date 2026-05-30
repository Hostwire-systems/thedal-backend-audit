package com.thedal.thedal_app.oauth2login;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.thedal.thedal_app.JwtService;
import com.thedal.thedal_app.account.AccountOnBoardStatus;
import com.thedal.thedal_app.user.UserEntity;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${thedal.ui.url}")
    private String thedalUIUrl;

    @Value("${thedal.domain.name}")
    private String thedalDomain;

    @Autowired
    private OAuthUserService userService;
    
    @Autowired
    private JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = token.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
       // String phoneNumber = oAuth2User.getAttribute("phone_number");
       System.out.println("OAuth login attempt for user: " + email);

        UserEntity user = userService.processOAuthPostLogin(email, name);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            // If user is inactive or not found, redirect them immediately
            String message = (user == null) ? "User not found." : "Your account is inactive. Please contact support.";
            System.out.println("Redirecting user to access-denied with message: " + message);
            redirectToAccessDenied(response, message);
            return; // Stop further processing like OTP, DB updates, etc.
        }
        String jwtToken = jwtService.generateAccessToken(user);
        
        String prefixedJwtToken = "Bearer " + jwtToken;
        // Ensure the token only has "Bearer " prefix once
        //String prefixedJwtToken = jwtToken.startsWith("Bearer ") ? jwtToken : "Bearer " + jwtToken;

        addCookie(response, "Authorization", prefixedJwtToken, 3600);
        addCookie(response, "userId", String.valueOf(user.getId()), 3600);
        addCookie(response, "email", user.getEmail(), 3600);
        //addCookie(response, "designBoardStatus", String.valueOf(user.getAccountEntity().getOnBoardStatus()), 3600);

        String redirectUrl = getRedirectUrl(user);
        // if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
        //     // If user is new or inactive, redirect to access-denied page with message
        //     redirectUrl = thedalUIUrl + "/access-denied?msg=" + URLEncoder.encode("Your account is in inactive status. Please contact support.", StandardCharsets.UTF_8);
        // }
        //  if(user.getAccountEntity().getOnBoardStatus()==AccountOnBoardStatus.OAUTH.getValue()){
        //     redirectUrl=thedalUIUrl + "/complete-sign-up" ;
        // }else{
        //     redirectUrl = thedalUIUrl + "/elections";
        // }

    //    String redirectUrl = user.getAccountEntity().getOnBoardStatus() == AccountOnBoardStatus.OAUTH.getValue() ? 
    //     		thedalUIUrl + "/complete-sign-up" : thedalUIUrl + "/election-dashboard";
        // response.sendRedirect(redirectUrl+"?t="+jwtToken);
        response.sendRedirect(redirectUrl + "?t=" + URLEncoder.encode(jwtToken, StandardCharsets.UTF_8));
        System.out.println("Redirecting user to: " + redirectUrl + "?t=" + jwtToken);
        //response.sendRedirect(thedalUIUrl + "/loginSuccessHandler");// ("http://localhost:3000/loginSuccessHandler");
    }
    private void redirectToAccessDenied(HttpServletResponse response, String message) throws IOException {
        // Log the error message to track reasons for denial
        System.out.println("User denied access with message: " + message);
        String errorUrl = thedalUIUrl + "/access-denied?msg=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
        response.sendRedirect(errorUrl);
    }
    private String getRedirectUrl(UserEntity user) {
        // If user has completed OAuth registration and is active, redirect to the next step.
        if (user.getAccountEntity().getOnBoardStatus() == AccountOnBoardStatus.OAUTH.getValue()) {
            // Redirect to a page for completing the sign-up process
            return thedalUIUrl + "/complete-sign-up";
        }
        
        // If user is already onboarded, redirect to the main election dashboard or whatever page you need
        return thedalUIUrl + "/elections";
    }
    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        try {
            String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
            Cookie cookie = new Cookie(name, encodedValue);
            cookie.setPath("/**");
            cookie.setHttpOnly(false);
            cookie.setSecure(true);
            cookie.setDomain(thedalDomain);
            response.addCookie(cookie);
            cookie.setMaxAge(maxAge);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }
}