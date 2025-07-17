package eu.europeana.metis.sandbox.common.locale;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Enum to represent languages available to add to a dataset process
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * This enum class is used as a database key and should NOT be changed *
 * without taking the existing database into account!                  *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 */
//todo MET-6691: This is a copy of the metis-core class, once the metis-core is used as orchestrator this class should be removed.
public enum Language {
  AR("Arabic"), AZ("Azerbaijani"), BE("Belarusian"), BG("Bulgarian"), BS(
      "Bosnian"), CA("Catalan"), CS("Czech"), CY("Welsh"), DA("Danish"), DE(
      "German"), EL("Greek"), EN("English"), ES("Spanish"), ET("Estonian"), EU(
      "Basque"), FI("Finnish"), FR("French"), GA("Irish"), GD("Gaelic (Scottish)"), GL(
      "Galician"), HE("Hebrew"), HI("Hindi"), HR("Croatian (hrvatski jezik)"), HU(
      "Hungarian"), HY("Armenian"), IE("Interlingue"), IS("Icelandic"), IT(
      "Italian"), JA("Japanese"), KA("Georgian"), KO("Korean"), LT("Lithuanian"), LV(
      "Latvian (Lettish)"), MK("Macedonian"), MT("Maltese"), MUL("Multilingual Content"), NL(
      "Dutch"), NO("Norwegian"), PL("Polish"), PT("Portuguese"), RO("Romanian"), RU(
      "Russian"), SK("Slovak"), SL("Slovenian"), SQ("Albanian"), SR("Serbian"), SV(
      "Swedish"), TR("Turkish"), UK("Ukrainian"), YI("Yiddish"), ZH("Chinese");
  private final String value;

  Language(String value) {
    this.value = value;
  }

  // The xmlValue of the language is used for the UI, NOT for the database
  @JsonValue
  public String xmlValue() {
    return value;
  }

  /**
   * Provides the languages sorted by the name field
   *
   * @return the list of languages sorted
   */
  public static List<Language> getLanguageListSortedByName() {
    List<Language> languages = Arrays
        .asList(Language.values());
    languages.sort(Comparator.comparing(Language::xmlValue));
    return languages;
  }

}
