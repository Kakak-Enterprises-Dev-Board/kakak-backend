package com.kakak.kakak_backend.Config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;
import java.util.UUID;

@Component
public class JwtUtil {
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpiration;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpiration;

    public String GenerateToken(String email) {
        return generateToken(email, ACCESS_TOKEN_TYPE, jwtExpiration);
    }

    public String GenerateRefreshToken(String email) {
        return generateToken(email, REFRESH_TOKEN_TYPE, refreshExpiration);
    }

    private String generateToken(String email, String tokenType, long expirationMs) {
        return Jwts
                .builder()
                .setId(UUID.randomUUID().toString()) // Unique per token
                .setSubject(email)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis() + expirationMs)
                )
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String Token) {
        return  extractClaim(Token,Claims::getSubject);
    }

    public Date extractExpiration(String Token){
        return extractClaim(Token,Claims::getExpiration);
    }

    public String extractTokenType(String Token) {
        return extractClaim(Token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    public <T> T extractClaim(String Token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(Token); // parse the token
        return claimsResolver.apply(claims);     // pick the field you want
    }
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    public boolean isTokenValid(String token, String email) {
        String extractedEmail = extractEmail(token);
        return extractedEmail.equals(email) && !isTokenExpired(token);
    }

    public boolean isAccessTokenValid(String token, String email) {
        return isTokenValid(token, email) && ACCESS_TOKEN_TYPE.equals(extractTokenType(token));
    }

    public boolean isRefreshTokenValid(String token, String email) {
        return isTokenValid(token, email) && REFRESH_TOKEN_TYPE.equals(extractTokenType(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
