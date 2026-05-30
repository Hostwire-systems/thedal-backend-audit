package com.thedal.thedal_app;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.auth.SecurityUserDetails;
import com.thedal.thedal_app.user.UserEntity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JwtService {

    @Autowired
    private Environment env;

    // run this to generate a new secret key
    // KeyGeneratorUtil.generateKey();

    private static Key secretKey;
    
    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpire;
    
    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpire;

    @PostConstruct
    public void init() {
        byte[] decodedKey = Base64.getDecoder().decode(env.getProperty("jwt.secret.key"));
        secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");
    }

    // method that takes in userEtity object and creates a jwt token for the user
    // the token is valid for 30 days
    public String generateAccessToken(UserEntity user) {
        SecurityUserDetails userDetails = new SecurityUserDetails(user);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", ((SecurityUserDetails) userDetails).getUserId());
        claims.put("accountId", user.getAccountEntity().getId());
        claims.put("roleId", user.getRole().getId());
        claims.put("Permission", user.getRole().getPermission());
        //claims.put("rolePermission", user.getRole().getRolePermission());
        claims.put("pwdVersion", user.getPasswordVersion());

        // setting the jwt token to expire after 30 days
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(Duration.ofDays(accessTokenExpire)))) // expires after 30 days
                .signWith(secretKey)
                .compact();
    }

    // Overload allowing session-specific identifiers (deviceId & jti) for active device tracking.
    public String generateAccessToken(UserEntity user, String deviceId, String jti) {
        SecurityUserDetails userDetails = new SecurityUserDetails(user);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("accountId", user.getAccountEntity().getId());
        claims.put("roleId", user.getRole().getId());
        claims.put("Permission", user.getRole().getPermission());
        claims.put("pwdVersion", user.getPasswordVersion());
        if (deviceId != null) claims.put("deviceId", deviceId);
        if (jti != null) claims.put("jti", jti);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(Date.from(Instant.now()))
                .setId(jti) // standard JWT ID header, optional
                .setExpiration(Date.from(Instant.now().plus(Duration.ofDays(accessTokenExpire))))
                .signWith(secretKey)
                .compact();
    }
    
    public String generateRefreshToken(UserEntity user) {
        SecurityUserDetails userDetails = new SecurityUserDetails(user);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", ((SecurityUserDetails) userDetails).getUserId());

        // setting the jwt token to expire after 30 days
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(Duration.ofDays(refreshTokenExpire)))) // expires after 60 days
                .signWith(secretKey)
                .compact();
    }

    public static String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public static Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey).build()
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.get("userId").toString());
    }

    public static boolean isValidToken(String token, UserEntity user) {
        SecurityUserDetails userDetails = new SecurityUserDetails(user);
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()));
    }

//    public static boolean validateToken(String jwt) {
//        try {
//            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(jwt);
//            return true;
//        } catch (ExpiredJwtException e) {
//            throw new ExpiredJwtException(null, null, "Expired JWT token");
//        } catch (MalformedJwtException e) {
//            throw new MalformedJwtException("Invalid JWT token");
//        }
//    }
    public static boolean validateToken(String jwt) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(jwt);
            return true;
        } catch (ExpiredJwtException ex) {
            log.error("Token expired", ex);
            throw ex;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Token validation failed", ex);
            throw new RuntimeException("Token validation failed", ex);
        }
    }
    
    public static Integer getPasswordVersionFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey).build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // Handle missing pwdVersion claim (for backward compatibility)
            Object pwdVersion = claims.get("pwdVersion");
            if (pwdVersion == null) {
                return 1; // Default version for old tokens
            }
            return Integer.parseInt(pwdVersion.toString());
        } catch (Exception e) {
            log.error("Error getting password version from token", e);
            return 1; // Default version if any error occurs
        }
    }

    public static String getDeviceId(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
            Object deviceId = claims.get("deviceId");
            return deviceId == null ? null : deviceId.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getJti(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
            // prefer explicit claim then standard jti
            Object jti = claims.get("jti");
            if (jti != null) return jti.toString();
            return claims.getId();
        } catch (Exception e) {
            return null;
        }
    }

}
