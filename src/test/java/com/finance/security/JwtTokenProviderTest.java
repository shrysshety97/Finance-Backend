package com.finance.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // 64-byte Base64-encoded secret (valid for HMAC-SHA256)
    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0cy1vbmx5LXRlc3Qtc2VjcmV0LWtleS0x";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 3_600_000L); // 1 hour
    }

    @Test
    @DisplayName("generateToken: produces a non-blank JWT string")
    void generateToken_returnsNonBlankToken() {
        String token = jwtTokenProvider.generateToken("testuser");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("getUsernameFromToken: extracts the correct username")
    void getUsernameFromToken_correctUsername() {
        String token = jwtTokenProvider.generateToken("alice");
        String extracted = jwtTokenProvider.getUsernameFromToken(token);
        assertThat(extracted).isEqualTo("alice");
    }

    @Test
    @DisplayName("validateToken: returns true for a fresh valid token")
    void validateToken_validToken_returnsTrue() {
        String token = jwtTokenProvider.generateToken("bob");
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken: returns false for a tampered token")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtTokenProvider.generateToken("charlie");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken: returns false for an expired token")
    void validateToken_expiredToken_returnsFalse() {
        // Set expiry to 1 millisecond
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 1L);
        String token = jwtTokenProvider.generateToken("diana");

        // Give it a tiny sleep to ensure expiry
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken: returns false for a blank/empty token")
    void validateToken_emptyToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
        assertThat(jwtTokenProvider.validateToken("   ")).isFalse();
    }
}
