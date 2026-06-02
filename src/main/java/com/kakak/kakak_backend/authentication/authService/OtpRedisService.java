package com.kakak.kakak_backend.authentication.authService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpRedisService {
    
    private static final int OTP_EXPIRATION_MINUTES = 5;
    private final RedisTemplate<String, String> redisTemplate;
    
    private String generateRedisKey(String phone, String purpose) {
        return "otp:" + phone + ":" + purpose;
    }
    
    public void storeOtp(String phone, String purpose, String otpHash) {
        String key = generateRedisKey(phone, purpose);
        redisTemplate.opsForValue().set(key, otpHash, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
    }
    
    public String getOtp(String phone, String purpose) {
        String key = generateRedisKey(phone, purpose);
        return redisTemplate.opsForValue().get(key);
    }
    
    public void deleteOtp(String phone, String purpose) {
        String key = generateRedisKey(phone, purpose);
        redisTemplate.delete(key);
    }
    
    public boolean existsOtp(String phone, String purpose) {
        String key = generateRedisKey(phone, purpose);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
