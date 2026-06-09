package com.kakak.kakak_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Slf4j
@Component
public class ApiLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        // Exclude Swagger, OpenAPI docs, and Actuator endpoints to reduce log noise
        if (uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs") || uri.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, 1024 * 1024);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequestResponse(requestWrapper, responseWrapper, duration);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequestResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString() != null ? "?" + request.getQueryString() : "";
        String clientIp = request.getRemoteAddr();
        int status = response.getStatus();

        // Get authenticated user
        String username = "anonymous";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            username = auth.getName();
        }

        // Get and sanitize request body
        String reqBody = getBody(request.getContentAsByteArray(), request.getCharacterEncoding());
        if (uri.contains("/login") || uri.contains("/register") || uri.contains("/change-password") || uri.contains("/reset-password") || uri.contains("/refresh")) {
            reqBody = "[PROTECTED/SENSITIVE]";
        } else if (reqBody.length() > 500) {
            reqBody = reqBody.substring(0, 500) + "... [truncated]";
        }

        // Get and sanitize response body
        String resBody = getBody(response.getContentAsByteArray(), response.getCharacterEncoding());
        if (uri.contains("/login") || uri.contains("/register") || uri.contains("/refresh")) {
            resBody = "[PROTECTED/SENSITIVE]";
        } else if (resBody.length() > 500) {
            resBody = resBody.substring(0, 500) + "... [truncated]";
        }

        log.info("API CALL | Method: {} | URI: {}{} | Status: {} | Time: {}ms | IP: {} | User: {} | Req: {} | Res: {}",
                method, uri, query, status, duration, clientIp, username, 
                reqBody.replaceAll("\\s+", " ").trim(), 
                resBody.replaceAll("\\s+", " ").trim());
    }

    private String getBody(byte[] buf, String encoding) {
        if (buf == null || buf.length == 0) return "";
        try {
            return new String(buf, 0, buf.length, encoding != null ? encoding : "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "[Unsupported Encoding]";
        }
    }
}
