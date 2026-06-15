package com.system.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BcryptTest {

    @Test
    public void testHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";
        String existingHash = "$2a$10$XptfOC1554U4.E1M6Mecae4tI3V4t7dD5EepmR573oRjEw3589z0G";
        
        System.out.println("Generated Hash for admin123: " + encoder.encode(rawPassword));
        assertTrue(encoder.matches(rawPassword, existingHash), "Hash does not match raw password");
    }
}
