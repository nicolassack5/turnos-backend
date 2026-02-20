package com.nico.turnos.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
    }

    private Key getSigningKeyViaReflection() {
        try {
            Field secretField = JwtService.class.getDeclaredField("SECRET_KEY");
            secretField.setAccessible(true);
            String secret = (String) secretField.get(null);
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private UserDetails mockUser(String username) {
        UserDetails user = Mockito.mock(UserDetails.class);
        when(user.getUsername()).thenReturn(username);
        return user;
    }

    @Test
    @DisplayName("Should generate a valid token containing the username as subject")
    void generateToken_containsUsernameAndIsParsable() {
        UserDetails user = mockUser("alice");

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        String subject = jwtService.extractUsername(token);
        assertEquals("alice", subject);
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    @DisplayName("Should validate token for matching user and invalidate for mismatched user")
    void isTokenValid_matchesAndMismatches() {
        UserDetails alice = mockUser("alice");
        UserDetails bob = mockUser("bob");

        String token = jwtService.generateToken(alice);

        assertTrue(jwtService.isTokenValid(token, alice), "Token should be valid for its owner");
        assertFalse(jwtService.isTokenValid(token, bob), "Token should be invalid for a different user");
    }

    @Test
    @DisplayName("Should return false for an expired token")
    void isTokenValid_expiredToken() {
        Key key = getSigningKeyViaReflection();

        Date now = new Date();
        Date past = new Date(now.getTime() - 1000L * 60); // expired 1 minute ago

        String expiredToken = Jwts.builder()
                .setSubject("charlie")
                .setIssuedAt(new Date(past.getTime() - 1000L))
                .setExpiration(past)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        UserDetails user = mockUser("charlie");
        assertFalse(jwtService.isTokenValid(expiredToken, user));
    }

    @Test
    @DisplayName("Should extract username (subject) from token")
    void extractUsername_returnsSubject() {
        UserDetails user = mockUser("dana");
        String token = jwtService.generateToken(user);

        String username = jwtService.extractUsername(token);
        assertEquals("dana", username);
    }

    @Test
    @DisplayName("Should set expiration about 24 hours ahead when generating tokens")
    void generateToken_has24hExpirationWindow() {
        UserDetails user = mockUser("eve");
        long before = System.currentTimeMillis();
        String token = jwtService.generateToken(user);
        long after = System.currentTimeMillis();

        Date exp = jwtService.extractClaim(token, Claims::getExpiration);
        assertNotNull(exp);

        long expMillis = exp.getTime();
        long expectedLower = before + 1000L * 60 * 60 * 24 - 1000L * 5; // allow a 5s clock/build tolerance
        long expectedUpper = after + 1000L * 60 * 60 * 24 + 1000L * 5;

        assertTrue(expMillis >= expectedLower && expMillis <= expectedUpper,
                "Expiration should be ~24h from issuance within tolerance");
    }

    @Test
    @DisplayName("Should include extra claims provided when generating token")
    void generateToken_includesExtraClaims() {
        UserDetails user = mockUser("frank");
        Map<String, Object> extra = new HashMap<>();
        extra.put("role", "ADMIN");
        extra.put("custom", 42);

        String token = jwtService.generateToken(extra, user);
        Claims claims = jwtService.extractClaim(token, c -> c);

        assertEquals("ADMIN", claims.get("role"));
        assertEquals(42, claims.get("custom"));
        assertEquals("frank", claims.getSubject());
    }

    @Test
    @DisplayName("Should throw when extracting claims from a token with invalid signature")
    void extractClaim_throwsOnInvalidSignature() {
        // Build a valid token first
        UserDetails user = mockUser("grace");
        String token = jwtService.generateToken(user);

        // Tamper token by altering a character in the payload segment to break the signature
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts");
        // flip last char of payload safely
        String payload = parts[1];
        char last = payload.charAt(payload.length() - 1);
        char flipped = last != 'a' ? 'a' : 'b';
        String tamperedPayload = payload.substring(0, payload.length() - 1) + flipped;
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThrows(RuntimeException.class, () -> jwtService.extractUsername(tampered));
    }

    @Test
    @DisplayName("Should return false when token subject is null while user has username")
    void isTokenValid_nullSubjectVsUser() {
        Key key = getSigningKeyViaReflection();
        String token = Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        UserDetails user = mockUser("henry");
        assertFalse(jwtService.isTokenValid(token, user));
    }

    @Test
    @DisplayName("Should allow extracting arbitrary claims via extractor function")
    void extractClaim_customExtractor() {
        UserDetails user = mockUser("irene");
        Map<String, Object> extra = new HashMap<>();
        extra.put("dept", "IT");
        String token = jwtService.generateToken(extra, user);

        String dept = jwtService.extractClaim(token, claims -> (String) claims.get("dept"));
        assertEquals("IT", dept);
    }

    @Test
    @DisplayName("Should produce different tokens for different users and same issue time window")
    void generateToken_tokensDifferForDifferentUsers() {
        UserDetails u1 = mockUser("jack");
        UserDetails u2 = mockUser("kate");

        String t1 = jwtService.generateToken(u1);
        String t2 = jwtService.generateToken(u2);

        assertNotEquals(t1, t2);
        assertEquals("jack", jwtService.extractUsername(t1));
        assertEquals("kate", jwtService.extractUsername(t2));
    }

    @Test
    @DisplayName("Should return false when token is expired even if username matches")
    void isTokenValid_expiredButMatchingUser() {
        Key key = getSigningKeyViaReflection();
        Date now = new Date();
        Date exp = new Date(now.getTime() - 2000L);
        String token = Jwts.builder()
                .setSubject("laura")
                .setIssuedAt(new Date(exp.getTime() - 1000L))
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        UserDetails user = mockUser("laura");
        assertFalse(jwtService.isTokenValid(token, user));
    }

    @Test
    @DisplayName("Should fail to parse a malformed token (not 3 parts)")
    void extractUsername_malformedToken() {
        String malformed = "abc.def"; // only 2 parts
        assertThrows(RuntimeException.class, () -> jwtService.extractUsername(malformed));
    }
}
