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
public enum Language {
  AR("ar"), AZ("az"), BE("be"), BG("bg"), BS("bs"), CA("ca"), CS("cs"), CY(
      "cy"), DA("da"), DE("de"), EL("el"), EN("en"), ES("es"), ET("et"), EU(
      "eu"), FI("fi"), FR("fr"), GA("ga"), GD("gd"), GL("gl"), HE("he"), HI(
      "hi"), HR("hr"), HU("hu"), HY("hy"), IE("ie"), IS("is"), IT("it"), JA(
      "ja"), KA("ka"), KO("ko"), LT("lt"), LV("lv"), MK("mk"), MT("mt"), MUL(
      "mul"), NL("nl"), NO("no"), PL("pl"), PT("pt"), RO("ro"), RU("ru"), SK(
      "sk"), SL("sl"), SQ("sq"), SR("sr"), SV("sv"), TR("tr"), UK("uk"), YI(
      "yi"), ZH("zh");
  private final String value;

  Language(String value) {
    this.value = value;
  }

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
