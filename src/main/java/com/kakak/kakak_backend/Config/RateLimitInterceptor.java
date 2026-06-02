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
        String clientIp = getClientIp(request);

        // Only apply rate limiting to POST requests on auth endpoints
        if (!method.equals("POST") || !uri.startsWith("/api/v1/auth/")) {
            return true;
        }

        boolean allowed = false;

        if (uri.contains("/send-otp")) {
            allowed = rateLimitService.isAllowed(clientIp, "send-otp", SEND_OTP_LIMIT, 1);
        } else if (uri.contains("/verify-otp")) {
            allowed = rateLimitService.isAllowed(clientIp, "verify-otp", VERIFY_OTP_LIMIT, 1);
        } else if (uri.contains("/register")) {
            allowed = rateLimitService.isAllowed(clientIp, "register", REGISTER_LIMIT, 1);
        } else if (uri.contains("/refresh")) {
            allowed = rateLimitService.isAllowed(clientIp, "refresh", REFRESH_LIMIT, 1);
        }

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Rate limit exceeded. Please try again later.\"}");
            return false;
        }

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
