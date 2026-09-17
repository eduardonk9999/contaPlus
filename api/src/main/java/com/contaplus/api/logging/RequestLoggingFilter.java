package com.contaplus.api.logging;

import com.contaplus.api.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final String USER_EMAIL = "userEmail";
    private static final String STORE_ID = "storeId";
    private static final String CLIENT_IP = "clientIp";
    private static final String METHOD = "method";
    private static final String PATH = "path";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            setupMDC(request);

            String requestId = MDC.get(REQUEST_ID);
            response.addHeader("X-Request-Id", requestId);

            log.info("Request started");

            filterChain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("durationMs", String.valueOf(duration));
            MDC.put("status", String.valueOf(response.getStatus()));

            log.info("Request completed");

            MDC.clear();
        }
    }

    private void setupMDC(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put(REQUEST_ID, requestId);
        MDC.put(METHOD, request.getMethod());
        MDC.put(PATH, request.getRequestURI());
        MDC.put(CLIENT_IP, resolveClientIp(request));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            MDC.put(USER_ID, user.getId().toString());
            MDC.put(USER_EMAIL, user.getEmail());
            if (user.getStore() != null) {
                MDC.put(STORE_ID, user.getStore().getId().toString());
            }
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
