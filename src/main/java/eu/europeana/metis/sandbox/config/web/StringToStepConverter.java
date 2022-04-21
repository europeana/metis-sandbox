package eu.europeana.metis.sandbox.config.web;

import eu.europeana.metis.sandbox.common.Step;
import org.springframework.core.convert.converter.Converter;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringToStepConverter implements Converter<String, Step> {

    private static final Map<String, Step> MAP = Stream
            .of(Step.values())
            .collect(Collectors.toMap(Step::value, Function.identity()));

    @Override
    public Step convert(String source) {
        return Optional
                .ofNullable(MAP.get(source))
                .orElseThrow(() -> new IllegalArgumentException(source));
    }
}
