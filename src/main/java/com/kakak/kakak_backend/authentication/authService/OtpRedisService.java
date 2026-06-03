package com.kakak.kakak_backend.authentication.authService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpRedisService {
    
    private static final int OTP_EXPIRATION_MINUTES = 1;
    private final RedisTemplate<String, String> redisTemplate;
    
    private String generateRedisKeyPlain(String phone, String purpose) {
        return "otp:plain:" + phone + ":" + purpose;
    }
    
    private String generateRedisKeyHash(String phone, String purpose) {
        return "otp:hash:" + phone + ":" + purpose;
    }
    
    /**
     * Store both plain OTP (for sending to user) and hashed OTP (for verification)
     */
    public void storeOtp(String phone, String purpose, String plainOtp, String otpHash) {
        String keyPlain = generateRedisKeyPlain(phone, purpose);
        String keyHash = generateRedisKeyHash(phone, purpose);
        redisTemplate.opsForValue().set(keyPlain, plainOtp, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(keyHash, otpHash, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
    }
    
    /**
     * Get the plain OTP to send to user (returns same OTP if already exists)
     */
    public String getPlainOtp(String phone, String purpose) {
        String key = generateRedisKeyPlain(phone, purpose);
        return redisTemplate.opsForValue().get(key);
    }
    
    /**
     * Get the hashed OTP for verification
     */
    public String getHashedOtp(String phone, String purpose) {
        String key = generateRedisKeyHash(phone, purpose);
        return redisTemplate.opsForValue().get(key);
    }
    
    public void deleteOtp(String phone, String purpose) {
        String keyPlain = generateRedisKeyPlain(phone, purpose);
        String keyHash = generateRedisKeyHash(phone, purpose);
        redisTemplate.delete(keyPlain);
        redisTemplate.delete(keyHash);
    }
    
    public boolean existsOtp(String phone, String purpose) {
        String key = generateRedisKeyPlain(phone, purpose);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
