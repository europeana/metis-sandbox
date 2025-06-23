package eu.europeana.metis.sandbox.service.debias;

import java.util.Arrays;
import java.util.Objects;

/**
 * The enum DeBias supported language.
 */
public enum DeBiasSupportedLanguage {
  /**
   * English DeBias supported language.
   */
  ENGLISH("en"),
  /**
   * Italian DeBias supported language.
   */
  ITALIAN("it"),
  /**
   * German DeBias supported language.
   */
  GERMAN("de"),
  /**
   * Dutch DeBias supported language.
   */
  DUTCH("nl"),
  /**
   * French DeBias supported language.
   */
  FRENCH("fr");

  private final String codeISO6391;

  DeBiasSupportedLanguage(String codeISO6391) {
    this.codeISO6391 = codeISO6391;
  }

  /**
   * Match DeBias supported languages.
   *
   * @param language the language
   * @return the DeBias supported language
   */
  public static DeBiasSupportedLanguage match(String language) {
    Objects.requireNonNull(language, "Language is required");
    final String mainLanguage = language.split("-")[0];
    return Arrays.stream(DeBiasSupportedLanguage.values())
                 .filter(lang -> lang.codeISO6391.equals(mainLanguage))
                 .findAny()
                 .orElse(null);
  }

  /**
   * Gets code iso 6391.
   *
   * @return the code iso 6391
   */
  public String getCodeISO6391() {
    return codeISO6391;
  }
}
