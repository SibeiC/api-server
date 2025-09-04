package com.chencraft.common.config;

import com.chencraft.common.service.executor.DefaultTaskExecutor;
import com.chencraft.common.service.executor.TaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorConfig {
    @Bean(destroyMethod = "shutdown")
    public TaskExecutor taskExecutor() {
        return new DefaultTaskExecutor(Runtime.getRuntime().availableProcessors());
    }
}