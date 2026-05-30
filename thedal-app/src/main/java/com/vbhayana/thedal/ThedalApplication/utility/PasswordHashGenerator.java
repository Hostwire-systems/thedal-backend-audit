package com.vbhayana.thedal.ThedalApplication.utility;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        
        System.out.println("=== BCrypt Password Hashes (Strength 10) ===\n");
        
        String[] passwords = {
            "Admk@123",
            "Dmk@123", 
            "Inc@123",
            "Bjp@123",
            "Ntk@123",
            "Tvk@123"
        };
        
        for (String password : passwords) {
            String hash = encoder.encode(password);
            System.out.println(password + " -> " + hash);
        }
    }
}
