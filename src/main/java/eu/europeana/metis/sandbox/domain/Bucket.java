package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

/**
 * Immutable object to store bucket name
 */
public class Bucket {

  private final String name;

  /**
   * Constructor
   * @param name must not be null
   * @throws NullPointerException if name is null
   */
  public Bucket(String name) {
    requireNonNull(name, "Name must not be null");
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
