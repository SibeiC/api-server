package com.chencraft.configuration;

import jakarta.annotation.Nonnull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-09T11:04:45.539601364Z[Etc/UTC]")
@Configuration
public class SwaggerUiConfiguration implements WebMvcConfigurer {
    @Override
    public void addViewControllers(@Nonnull ViewControllerRegistry registry) {
        registry.addViewController("/swagger-ui/").setViewName("forward:/swagger-ui/index.html");
    }
}
