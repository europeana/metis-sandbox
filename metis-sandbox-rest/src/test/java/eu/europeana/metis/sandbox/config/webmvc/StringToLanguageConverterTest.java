package eu.europeana.metis.sandbox.config.webmvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.europeana.metis.sandbox.common.locale.Language;
import org.junit.jupiter.api.Test;

class StringToLanguageConverterTest {

  private final StringToLanguageConverter converter = new StringToLanguageConverter();

  @Test
  void convert_expectSuccess() {
    Language language = converter.convert("Arabic");
    assertEquals(Language.AR, language);
  }

  @Test
  void convert_invalidCountry_expectFail() {
    assertThrows(IllegalArgumentException.class, () -> converter.convert("None"));
  }
}