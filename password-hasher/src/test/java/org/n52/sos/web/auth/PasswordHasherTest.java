package org.n52.sos.web.auth;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHasherTest {
    public String password = "password";
    public String hashedPasswordForPassword = "$2a$10$i4pG0A56sQJpbmNoOvRxoOfWVQY2PNaiF9W0py9LEkwY3pbDqrsru";
    
    @Test
    public void testHasher() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertTrue(encoder.matches(password, hashedPasswordForPassword));
        assertTrue(encoder.matches(password, PasswordHasher.hashPassword(password)));
    }
}
