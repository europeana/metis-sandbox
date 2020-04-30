package eu.europeana.metis.sandbox.config.web;

import static org.junit.jupiter.api.Assertions.*;

import eu.europeana.metis.sandbox.common.locale.Country;
import org.junit.jupiter.api.Test;

class StringToCountryConverterTest {

  private final StringToCountryConverter converter = new StringToCountryConverter();

  @Test
  void convert_expectSuccess() {
    Country country = converter.convert("Netherlands");
    assertEquals(Country.NETHERLANDS, country);
  }

  @Test
  void convert_invalidCountry_expectFail() {
    assertThrows(IllegalArgumentException.class, () -> converter.convert("None"));
  }
}