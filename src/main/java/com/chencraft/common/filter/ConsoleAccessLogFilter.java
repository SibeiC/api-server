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
public class ConsoleAccessLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        long start = System.currentTimeMillis();
        filterChain.doFilter(request, response);
        long duration = System.currentTimeMillis() - start;

        log.info("| {} | {} | {} | {} | {} |",
                 padRight(request.getRemoteAddr(), 15),
                 padRight(request.getMethod(), 6),
                 padRight(request.getRequestURI(), 60),
                 response.getStatus(),
                 padRight(duration + "ms", 6));
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}