package eu.europeana.metis.sandbox.common;

import lombok.Getter;

/**
 * Enum representing the harvest protocols.
 */
@Getter
public enum HarvestProtocol {
  HTTP(Values.HTTP),
  FILE(Values.FILE),
  OAI(Values.OAI);

  private final String value;

  HarvestProtocol(String value) {
    //Enforce equality between the enum constant name and the string value.
    if (!this.name().equals(value)) {
      throw new IllegalArgumentException("Incorrect use of ELanguage");
    }
    this.value = value;
  }

  /**
   * Contains constant string values representing different harvest protocols.
   * <p>
   * These constants are used as discriminators.
   */
  public static final class Values {

    public static final String HTTP = "HTTP";
    public static final String FILE = "FILE";
    public static final String OAI = "OAI";

    private Values() {
    }
  }
}

