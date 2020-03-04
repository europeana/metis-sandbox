package eu.europeana.metis.sandbox.common.locale;

public enum Country {
  ALBANIA("Albania"), ANDORRA("Andorra"), ARMENIA("Armenia"), AUSTRIA(
      "Austria"), AZERBAIJAN("Azerbaijan"), AUSTRALIA("Australia"), BELARUS(
      "Belarus"), BELGIUM("Belgium"), BOSNIAAND_HERZEGOVINA(
      "Bosnia and Herzegovina"), BULGARIA("Bulgaria"), CANADA("Canada"), CHINA(
      "China"), CROATIA("Croatia"), CYPRUS("Cyprus"), CZECH_REPUBLIC(
      "Czech Republic"), DENMARK("Denmark"), ESTONIA("Estonia"), EUROPE(
      "Europe"), FINLAND("Finland"), FRANCE("France"), GEORGIA("Georgia"), GERMANY(
      "Germany"), GREECE("Greece"), HOLY_SEE_VATICAN_CITY_STATE(
      "Holy See (Vatican City State)"), HUNGARY("Hungary"), ICELAND(
      "Iceland"), INDIA("India"), IRELAND("Ireland"), ITALY("Italy"), ISRAEL(
      "Israel"), JAPAN("Japan"), KAZAKHSTAN("Kazakhstan"), KOREA_REPUBLICOF(
      "Korea, Republic of"), LATVIA("Latvia"), LEBANON("Lebanon"), LIECHTENSTEIN(
      "Liechtenstein"), LITHUANIA("Lithuania"), LUXEMBOURG("Luxembourg"), MACEDONIA(
      "Macedonia"), MALTA("Malta"), MOLDOVA("Moldova"), MONACO("Monaco"), MONTENEGRO(
      "Montenegro"), NETHERLANDS("Netherlands"), NORWAY("Norway"), POLAND(
      "Poland"), PORTUGAL("Portugal"), ROMANIA("Romania"), RUSSIA(
      "Russia"), SAN_MARINO("San Marino"), SERBIA("Serbia"), SLOVAKIA(
      "Slovakia"), SLOVENIA("Slovenia"), SPAIN("Spain"), SWEDEN("Sweden"), SWITZERLAND(
      "Switzerland"), TURKEY("Turkey"), UKRAINE("Ukraine"), UNITED_KINGDOM(
      "United Kingdom"), UNITED_STATESOF_AMERICA(
      "United States of America");
  private final String value;

  Country(String value) {
    this.value = value;
  }

  public String xmlValue() {
    return value;
  }

  public static Country convert(String value) {
    for (Country inst : values()) {
      if (inst.xmlValue().equals(value)) {
        return inst;
      }
    }
    return null;
  }
}