package com.kakak.kakak_backend.authentication.authService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakak.kakak_backend.authentication.authEntity.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtUtil {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final ObjectMapper objectMapper;

    @Value("${security.jwt.secret:change-me-in-production-change-me-in-production}")
    private String secret;

    @Value("${security.jwt.access-expiration-seconds:900}")
    private long accessTokenExpirationSeconds;

    @Value("${security.jwt.refresh-expiration-seconds:604800}")
    private long refreshTokenExpirationSeconds;

    @Value("${security.jwt.remember-me-refresh-expiration-seconds:2592000}")
    private long rememberMeRefreshTokenExpirationSeconds;

    public JwtUtil() {
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("security.jwt.secret must be configured");
        }
    }

    public String generateAccessToken(User user, boolean rememberMe) {
        return buildToken(user, TOKEN_TYPE_ACCESS, Duration.ofSeconds(accessTokenExpirationSeconds), rememberMe, null);
    }

    public String generateAccessToken(User user, String sessionId, boolean rememberMe) {
        return buildToken(user, TOKEN_TYPE_ACCESS, Duration.ofSeconds(accessTokenExpirationSeconds), rememberMe, sessionId);
    }

    public String generateRefreshToken(User user, boolean rememberMe) {
        long expiresInSeconds = getRefreshTokenExpirationSeconds(rememberMe);
        return buildToken(user, TOKEN_TYPE_REFRESH, Duration.ofSeconds(expiresInSeconds), rememberMe, null);
    }

    public String generateRefreshToken(User user, String sessionId, boolean rememberMe) {
        long expiresInSeconds = getRefreshTokenExpirationSeconds(rememberMe);
        return buildToken(user, TOKEN_TYPE_REFRESH, Duration.ofSeconds(expiresInSeconds), rememberMe, sessionId);
    }

    public boolean isTokenValid(String token) {
        try {
            Map<String, Object> claims = parseClaims(token);
            Object expiration = claims.get("exp");
            if (!(expiration instanceof Number number)) {
                return false;
            }
            return Instant.now().getEpochSecond() < number.longValue();
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isAccessTokenValid(String token) {
        try {
            Map<String, Object> claims = parseClaims(token);
            Object type = claims.get("type");
            return TOKEN_TYPE_ACCESS.equals(type)
                    && isTokenValid(token);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            Map<String, Object> claims = parseClaims(token);
            Object type = claims.get("type");
            return TOKEN_TYPE_REFRESH.equals(type)
                    && isTokenValid(token);
        } catch (Exception ex) {
            return false;
        }
    }

    public String extractUsername(String token) {
        Object subject = parseClaims(token).get("sub");
        return subject == null ? null : String.valueOf(subject);
    }

    public String extractRole(String token) {
        Object role = parseClaims(token).get("role");
        return role == null ? null : String.valueOf(role);
    }

    public String extractSessionId(String token) {
        Object sessionId = parseClaims(token).get("sid");
        return sessionId == null ? null : String.valueOf(sessionId);
    }

    public String extractTokenType(String token) {
        Object type = parseClaims(token).get("type");
        return type == null ? null : String.valueOf(type);
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationSeconds(boolean rememberMe) {
        return rememberMe ? rememberMeRefreshTokenExpirationSeconds : refreshTokenExpirationSeconds;
    }

    private String buildToken(User user, String tokenType, Duration expiration, boolean rememberMe, String sessionId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiration);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getUsername());
        payload.put("userId", user.getId() == null ? null : user.getId().toString());
        payload.put("email", user.getEmail());
        payload.put("role", user.getRole() == null ? null : user.getRole().getName());
        payload.put("status", user.getStatus());
        payload.put("type", tokenType);
        payload.put("sid", sessionId);
        payload.put("rememberMe", rememberMe);
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        try {
            String encodedHeader = base64Url(objectMapper.writeValueAsString(header));
            String encodedPayload = base64Url(objectMapper.writeValueAsString(payload));
            String signingInput = encodedHeader + "." + encodedPayload;
            String signature = base64Url(sign(signingInput));
            return signingInput + "." + signature;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build token", e);
        }
    }

    private byte[] sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign token", e);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private Map<String, Object> parseClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token");
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = base64Url(sign(signingInput));
            if (!expectedSignature.equals(parts[2])) {
                throw new IllegalArgumentException("Invalid token signature");
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> claims = objectMapper.readValue(payloadJson, LinkedHashMap.class);
            return claims == null ? Collections.emptyMap() : claims;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }
}
