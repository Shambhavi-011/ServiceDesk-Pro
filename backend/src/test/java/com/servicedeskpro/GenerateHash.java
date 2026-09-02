package com.servicedeskpro;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String hash = encoder.encode("Admin@123");
        System.out.println("=== BCrypt Hash for Admin@123 ===");
        System.out.println(hash);
        System.out.println("=================================");
        // Verify
        System.out.println("Verify: " + encoder.matches("Admin@123", hash));
    }
}