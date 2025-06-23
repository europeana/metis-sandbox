package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.utils.CustomTruststoreAppender;
import jakarta.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import eu.europeana.metis.common.config.properties.TruststoreConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up a custom truststore.
 *
 * <p>Upon initialization, indicated by the {@link PostConstruct} annotation, it attempts to append a custom truststore to the
 * default Java truststore using a provided path and password from {@link TruststoreConfigurationProperties}.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({TruststoreConfigurationProperties.class})
class TruststoreConfig {

  private final TruststoreConfigurationProperties truststoreConfigurationProperties;

  /**
   * Constructs an instance of TruststoreConfig with the specified truststore configuration properties.
   *
   * @param truststoreConfigurationProperties the configuration properties for the truststore,
   *                                           containing details such as path and password
   */
  TruststoreConfig(TruststoreConfigurationProperties truststoreConfigurationProperties) {
    this.truststoreConfigurationProperties = truststoreConfigurationProperties;
  }

  /**
   * Initializes the custom truststore configuration for the application.
   * This method is invoked after the dependency injection is completed, as denoted by the {@link PostConstruct} annotation.
   * It delegates the initialization to the {@code initializeTruststore} method, which validates and appends
   * the custom truststore configured via {@link TruststoreConfigurationProperties} to the default truststore.
   *
   * @throws CustomTruststoreAppender.TrustStoreConfigurationException if there is an error during the initialization and appending
   *                                                                   of the custom truststore.
   */
  @PostConstruct
  void init() throws CustomTruststoreAppender.TrustStoreConfigurationException {
    initializeTruststore(truststoreConfigurationProperties);
  }

  private void initializeTruststore(TruststoreConfigurationProperties truststoreConfigurationProperties)
      throws CustomTruststoreAppender.TrustStoreConfigurationException {
    if (StringUtils.isNotEmpty(truststoreConfigurationProperties.getPath()) &&
        StringUtils.isNotEmpty(truststoreConfigurationProperties.getPassword())) {
      CustomTruststoreAppender.appendCustomTruststoreToDefault(truststoreConfigurationProperties.getPath(),
          truststoreConfigurationProperties.getPassword());
      log.info("Custom truststore appended to default truststore");
    }
  }
}
