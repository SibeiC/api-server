package com.chencraft.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurePathConfig implements WebMvcConfigurer {
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Add prefix /secure to eligible controllers
        configurer.addPathPrefix("secure",
                                 c -> c.getPackageName().startsWith("com.chencraft.api.secure"));
    }
}
