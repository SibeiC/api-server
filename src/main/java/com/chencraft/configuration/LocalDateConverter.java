package com.chencraft.configuration;

import org.springframework.core.convert.converter.Converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record LocalDateConverter(DateTimeFormatter formatter) implements Converter<String, LocalDate> {
    @Override
    public LocalDate convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        return LocalDate.parse(source, this.formatter);
    }
}
