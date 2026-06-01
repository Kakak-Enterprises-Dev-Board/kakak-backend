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

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration-ms}")
    private String jwtExpiration;

    public String GenerateToken(String email) {
        return Jwts
                .builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis()+ jwtExpiration)
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
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    }
