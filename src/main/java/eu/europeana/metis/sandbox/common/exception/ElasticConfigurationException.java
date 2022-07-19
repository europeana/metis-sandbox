package eu.europeana.metis.sandbox.common.exception;

public class ElasticConfigurationException extends RuntimeException {

  private static final long serialVersionUID = -7106212217032816475L;

  public ElasticConfigurationException(String message, Throwable cause) {
    super(message,cause);
  }
}
