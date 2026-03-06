package com.ratelimiter.auth;

import com.ratelimiter.model.ApiKeyInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that extracts the X-API-Key header and attaches the resolved
 * {@link ApiKeyInfo} as a request attribute for downstream use.
 */
@Component
@Order(1)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String API_KEY_ATTR = "apiKeyInfo";

    private final ApiKeyStore apiKeyStore;

    public ApiKeyAuthFilter(ApiKeyStore apiKeyStore) {
        this.apiKeyStore = apiKeyStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey != null && !apiKey.isBlank()) {
            ApiKeyInfo info = apiKeyStore.validate(apiKey);
            if (info != null) {
                request.setAttribute(API_KEY_ATTR, info);
            }
        }

        filterChain.doFilter(request, response);
    }
}
