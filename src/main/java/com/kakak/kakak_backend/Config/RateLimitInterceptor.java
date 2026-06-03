package com.kakak.kakak_backend.Config;

import com.kakak.kakak_backend.authentication.authService.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    private static final int SEND_OTP_LIMIT = 3; // 3 attempts per minute
    private static final int VERIFY_OTP_LIMIT = 5; // 5 attempts per minute
    private static final int REGISTER_LIMIT = 5; // 5 attempts per minute per IP
    private static final int REFRESH_LIMIT = 10; // 10 attempts per minute per IP

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Only apply rate limiting to POST requests on auth endpoints
        if (!method.equals("POST") || !uri.startsWith("/api/v1/auth/")) {
            return true;
        }

        boolean allowed = true;

        if (uri.contains("/send-otp")) {
            return true;
        }

        // Rate limiting for other endpoints is now handled in service methods (per-user basis)
        // This interceptor is kept for potential additional checks

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
