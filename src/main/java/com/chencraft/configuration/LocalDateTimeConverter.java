package com.chencraft.configuration;

import org.springframework.core.convert.converter.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record LocalDateTimeConverter(DateTimeFormatter formatter) implements Converter<String, LocalDateTime> {
    @Override
    public LocalDateTime convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(source, this.formatter);
    }
}
