package eu.europeana.metis.sandbox.config.web;

import eu.europeana.metis.sandbox.common.locale.Country;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.convert.converter.Converter;

class StringToCountryConverter implements Converter<String, Country> {

  private static final Map<String, Country> MAP = Stream
      .of(Country.values())
      .collect(Collectors.toMap(Country::xmlValue, Function.identity()));

  @Override
  public Country convert(String source) {
    return Optional
        .ofNullable(MAP.get(source))
        .orElseThrow(() -> new IllegalArgumentException(source));
  }
}
