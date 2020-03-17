package eu.europeana.metis.sandbox.common.locale;

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

  public String xmlValue() {
    return value;
  }

  public static Language convert(String value) {
    for (Language inst : values()) {
      if (inst.xmlValue().equals(value)) {
        return inst;
      }
    }
    return null;
  }
}