package com.kakak.kakak_backend.authentication.authService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final int DEFAULT_RATE_LIMIT = 5; // requests
    private static final int DEFAULT_WINDOW_MINUTES = 1; // time window

    /**
     * Check if request is allowed based on IP address
     * @param ipAddress IP address of the client
     * @param endpoint API endpoint
     * @param limit Maximum allowed requests in the time window
     * @param windowMinutes Time window in minutes
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String ipAddress, String endpoint, int limit, int windowMinutes) {
        String key = generateKey(ipAddress, endpoint);
        return checkAndIncrement(key, limit, windowMinutes);
    }

    /**
     * Check if request is allowed with default rate limit
     * @param ipAddress IP address of the client
     * @param endpoint API endpoint
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String ipAddress, String endpoint) {
        return isAllowed(ipAddress, endpoint, DEFAULT_RATE_LIMIT, DEFAULT_WINDOW_MINUTES);
    }

    /**
     * Check rate limit for phone number (useful for OTP verification)
     * @param phone Phone number
     * @param purpose Purpose/endpoint identifier
     * @param limit Maximum attempts
     * @param windowMinutes Time window in minutes
     * @return true if attempt allowed, false if limit exceeded
     */
    public boolean isPhoneAllowed(String phone, String purpose, int limit, int windowMinutes) {
        String key = "ratelimit:phone:" + phone + ":" + purpose;
        return checkAndIncrement(key, limit, windowMinutes);
    }

    /**
     * Get remaining attempts for a key
     * @param ipAddress IP address
     * @param endpoint API endpoint
     * @return remaining attempts
     */
    public long getRemainingAttempts(String ipAddress, String endpoint) {
        String key = generateKey(ipAddress, endpoint);
        String current = redisTemplate.opsForValue().get(key);
        if (current == null) {
            return DEFAULT_RATE_LIMIT;
        }
        long count = Long.parseLong(current);
        return Math.max(0, DEFAULT_RATE_LIMIT - count);
    }

    /**
     * Reset rate limit for an IP/endpoint combination
     * @param ipAddress IP address
     * @param endpoint API endpoint
     */
    public void reset(String ipAddress, String endpoint) {
        String key = generateKey(ipAddress, endpoint);
        redisTemplate.delete(key);
    }

    /**
     * Reset rate limit for a phone number
     * @param phone Phone number
     * @param purpose Purpose/endpoint identifier
     */
    public void resetPhone(String phone, String purpose) {
        String key = "ratelimit:phone:" + phone + ":" + purpose;
        redisTemplate.delete(key);
    }

    private String generateKey(String ipAddress, String endpoint) {
        return "ratelimit:ip:" + ipAddress + ":" + endpoint;
    }

    private boolean checkAndIncrement(String key, int limit, int windowMinutes) {
        String current = redisTemplate.opsForValue().get(key);

        if (current == null) {
            redisTemplate.opsForValue().set(key, "1", windowMinutes, TimeUnit.MINUTES);
            return true;
        }

        long count = Long.parseLong(current);
        if (count >= limit) {
            return false;
        }

        redisTemplate.opsForValue().increment(key);
        return true;
    }
}
