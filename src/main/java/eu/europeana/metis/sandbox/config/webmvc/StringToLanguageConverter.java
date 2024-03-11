package eu.europeana.metis.sandbox.config.webmvc;

import eu.europeana.metis.sandbox.common.locale.Language;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.convert.converter.Converter;

/**
 * Converter to map language string value to enum
 */
class StringToLanguageConverter implements Converter<String, Language> {

  private static final Map<String, Language> MAP = Stream
      .of(Language.values())
      .collect(Collectors.toMap(Language::xmlValue, Function.identity()));

  @Override
  public Language convert(String source) {
    return Optional
        .ofNullable(MAP.get(source))
        .orElseThrow(() -> new IllegalArgumentException(source));
  }
}
