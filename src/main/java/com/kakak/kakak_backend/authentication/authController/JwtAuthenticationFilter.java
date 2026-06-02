package com.kakak.kakak_backend.authentication.authController;

import com.kakak.kakak_backend.authentication.authService.JwtUtil;
import com.kakak.kakak_backend.authentication.authRepository.authSessionRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final authSessionRepo sessionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            String requestUri = request.getRequestURI() == null ? "" : request.getRequestURI();
            boolean publicEndpoint = requestUri.endsWith("/api/v1/auth/login")
                    || requestUri.endsWith("/api/v1/auth/refresh")
                    || requestUri.startsWith("/swagger-ui")
                    || requestUri.startsWith("/v3/api-docs");
            if (jwtUtil.isAccessTokenValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);
                String sessionId = jwtUtil.extractSessionId(token);
                java.util.UUID sessionUuid = parseSessionId(sessionId);
                if (sessionUuid != null && sessionRepository.findByIdAndRevokedFalse(sessionUuid).isPresent()) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else if (!publicEndpoint) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                    return;
                }
            } else if (!publicEndpoint) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private java.util.UUID parseSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        try {
            return java.util.UUID.fromString(sessionId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
