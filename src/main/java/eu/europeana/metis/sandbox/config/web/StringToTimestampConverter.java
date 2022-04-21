package eu.europeana.metis.sandbox.config.web;

import org.springframework.core.convert.converter.Converter;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class StringToTimestampConverter implements Converter<String, LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    @Override
    public LocalDateTime convert(String source) {
        return Optional
                .of(LocalDateTime.from(FORMATTER.parse(source)))
                .orElseThrow(() -> new IllegalArgumentException(source));
    }
}
