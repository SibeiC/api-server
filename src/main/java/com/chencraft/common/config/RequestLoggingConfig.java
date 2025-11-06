package com.chencraft.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class RequestLoggingConfig {
    private static final Set<String> SKIP_LOGGING_HEADERS = Stream.of(
            "x-client-cert", "x-client-dn", "X-Client-Cert", "X-Client-DN"
    ).collect(Collectors.toSet());

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setIncludeHeaders(true);
        filter.setMaxPayloadLength(10000);
        filter.setHeaderPredicate(header -> !SKIP_LOGGING_HEADERS.contains(header));
        return filter;
    }
}
