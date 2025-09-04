package com.chencraft.common.filter;


import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class MtlsVerificationFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(@Nonnull HttpServletRequest request) {
        // Only apply filter to /secure/** endpoints
        return !request.getRequestURI().startsWith("/secure/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) throws ServletException, IOException {
        String verify = request.getHeader("X-Client-Verify");

        if (!"SUCCESS".equalsIgnoreCase(verify)) {
            log.warn("mTLS verification failed for {}: {}", request.getRequestURI(), verify);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "mTLS required");
            return;
        }

        // TODO: Check if client certificate is revoked

        filterChain.doFilter(request, response);
    }
}
