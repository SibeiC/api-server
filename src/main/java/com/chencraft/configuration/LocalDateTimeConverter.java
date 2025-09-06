package com.chencraft.configuration;

import org.springframework.core.convert.converter.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Spring Converter that parses String values into LocalDateTime using a provided DateTimeFormatter.
 * Accepts empty string as null to allow optional parameters and form fields.
 */
public record LocalDateTimeConverter(DateTimeFormatter formatter) implements Converter<String, LocalDateTime> {
    @Override
    public LocalDateTime convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(source, this.formatter);
    }
}
