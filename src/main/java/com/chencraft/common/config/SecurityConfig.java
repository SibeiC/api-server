package com.chencraft.common.config;


import com.chencraft.common.filter.MtlsVerificationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final MtlsVerificationFilter mtlsVerificationFilter;

    @Autowired
    public SecurityConfig(MtlsVerificationFilter mtlsVerificationFilter) {
        this.mtlsVerificationFilter = mtlsVerificationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)  // disable CSRF since I do not hand out CSRF token
            .addFilterBefore(mtlsVerificationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .preload(true)
                            .maxAgeInSeconds(31536000)  // 365 * 60 * 60 * 24
                    )
            );
        return http.build();
    }

    @Bean
    public AuthenticationManager noopAuthenticationManager() {
        return authentication -> {
            throw new AuthenticationServiceException("Password-based authentication is disabled");
        };
    }
}
