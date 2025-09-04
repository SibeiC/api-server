package com.chencraft.common.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
public class AppConfig {
    private final Environment env;

    @Autowired
    public AppConfig(Environment env) {
        this.env = env;
    }

    public boolean isDev() {
        return Arrays.asList(env.getActiveProfiles()).contains("dev");
    }
}
