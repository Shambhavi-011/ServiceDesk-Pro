package com.servicedeskpro;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Utility test — generates BCrypt hash and writes it to a file for retrieval.
 */
public class HashGeneratorTest {

    @Test
    void generateAdminHash() throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String hash = encoder.encode("Admin@123");
        boolean ok = encoder.matches("Admin@123", hash);
        String output = "HASH=" + hash + "\nVERIFY=" + ok;
        Files.writeString(Paths.get("target/admin_hash.txt"), output);
        System.out.println("Hash written to target/admin_hash.txt");
        System.out.println("Verify: " + ok);
    }
}
