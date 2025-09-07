package com.chencraft;

import com.chencraft.configuration.LocalDateConverter;
import com.chencraft.configuration.LocalDateTimeConverter;
import com.fasterxml.jackson.databind.Module;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.Serial;
import java.time.format.DateTimeFormatter;

/**
 * Main Spring Boot entry point for the API server.
 * <p>
 * Responsibilities:
 * - Bootstraps the Spring context and exposes MVC and reactive WebClient infrastructure.
 * - Registers JSON Nullable support for OpenAPI-generated models via JsonNullableModule.
 * - Configures strict date/time binding for Spring MVC through CustomDateConfig.
 * Thread-safety: relies on Spring-managed singletons; no mutable static state.
 * External IO: none in this class beyond application startup.
 */
@EnableCaching
@SpringBootApplication
public class Application implements CommandLineRunner {

    /**
     * Hook invoked after the application context has started.
     *
     * @param arg0 command-line arguments passed to the application; if the first argument equals "exitcode",
     *             an ExitException will be thrown to terminate with a deterministic exit code.
     * @throws Exception if application termination is requested.
     */
    @Override
    public void run(String... arg0) throws Exception {
        if (arg0.length > 0 && arg0[0].equals("exitcode")) {
            throw new ExitException();
        }
    }

    /**
         * Launches the Spring Boot application.
         *
         * @param args raw command-line arguments.
         */
        public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }

    /**
     * Registers support for org.openapitools.jackson.nullable.JsonNullable with Jackson.
     *
     * @return a Module enabling JsonNullable (used by OpenAPI-generated models to express optionality).
     */
    @Bean
    public Module jsonNullableModule() {
        return new JsonNullableModule();
    }

    /**
     * MVC configuration that registers strict date/time converters.
     * <p>
     * Accepted patterns:
     * - LocalDate: yyyy-MM-dd
     * - LocalDateTime: yyyy-MM-dd'T'HH:mm:ss.SSS
     */
    @Configuration
    static class CustomDateConfig implements WebMvcConfigurer {
        /**
         * Registers converter instances for LocalDate and LocalDateTime.
         *
         * @param registry Spring's FormatterRegistry receiving the converters.
         */
        @Override
        public void addFormatters(FormatterRegistry registry) {
            registry.addConverter(new LocalDateConverter(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            registry.addConverter(new LocalDateTimeConverter(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")));
        }
    }

    /**
         * Runtime exception used to terminate the app with a deterministic exit code (10) for scripts.
         */
        static class ExitException extends RuntimeException implements ExitCodeGenerator {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public int getExitCode() {
            return 10;
        }
    }
}
