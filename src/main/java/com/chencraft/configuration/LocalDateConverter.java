package com.chencraft.configuration;

import org.springframework.core.convert.converter.Converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Spring Converter that parses String values into LocalDate using a provided DateTimeFormatter.
 * Accepts empty string as null to let optional request params bind cleanly.
 */
public record LocalDateConverter(DateTimeFormatter formatter) implements Converter<String, LocalDate> {
    @Override
    public LocalDate convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        return LocalDate.parse(source, this.formatter);
    }
}
