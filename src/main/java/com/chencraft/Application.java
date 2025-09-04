package com.chencraft;

import com.chencraft.configuration.LocalDateConverter;
import com.chencraft.configuration.LocalDateTimeConverter;
import com.fasterxml.jackson.databind.Module;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.Serial;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
public class Application implements CommandLineRunner {

    @Override
    public void run(String... arg0) throws Exception {
        if (arg0.length > 0 && arg0[0].equals("exitcode")) {
            throw new ExitException();
        }
    }

    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }

    @Bean
    public Module jsonNullableModule() {
        return new JsonNullableModule();
    }

    @Configuration
    static class CustomDateConfig implements WebMvcConfigurer {
        @Override
        public void addFormatters(FormatterRegistry registry) {
            registry.addConverter(new LocalDateConverter(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            registry.addConverter(new LocalDateTimeConverter(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")));
        }
    }

    static class ExitException extends RuntimeException implements ExitCodeGenerator {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public int getExitCode() {
            return 10;
        }
    }
}
