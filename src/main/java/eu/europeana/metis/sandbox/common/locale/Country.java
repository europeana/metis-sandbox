package eu.europeana.metis.sandbox.common.locale;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Enum to represent countries available to add to a dataset process
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * This enum class is used as a database key and should NOT be changed *
 * without taking the existing database into account!                  *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 */
//todo MET-6691: This is a copy of the metis-core class, once the metis-core is used as orchestrator this class should be removed.
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
      "Liechtenstein"), LITHUANIA("Lithuania"), LUXEMBOURG("Luxembourg"), NORTH_MACEDONIA(
      "North Macedonia"), MALTA("Malta"), MOLDOVA("Moldova"), MONACO("Monaco"), MONTENEGRO(
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

  @JsonValue
  public String xmlValue() {
    return value;
  }

  /**
   * Provides the countries sorted by the name field
   *
   * @return the list of countries sorted
   */
  public static List<Country> getCountryListSortedByName() {
    List<Country> countries = Arrays
            .asList(Country.values());
    countries.sort(Comparator.comparing(Country::xmlValue));
    return countries;
  }

}
