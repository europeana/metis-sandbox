package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.utils.CustomTruststoreAppender;
import eu.europeana.metis.utils.CustomTruststoreAppender.TrustStoreConfigurationException;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the datasource, ensures that the custom trust store is appended before creating the
 * datasource
 */
@Configuration
class DataSourceConfig {

  @Value("${sandbox.truststore.path}")
  private String trustStorePath;

  @Value("${sandbox.truststore.password}")
  private String trustStorePassword;

  @Bean
  @ConfigurationProperties(prefix = "sandbox.datasource")
  public DataSource getDataSource() throws TrustStoreConfigurationException {
    appendCustomTrustStore();
    return DataSourceBuilder.create().build();
  }

  private void appendCustomTrustStore()
      throws TrustStoreConfigurationException {
    if (StringUtils.isNotEmpty(trustStorePath) && StringUtils.isNotEmpty(trustStorePassword)) {
      CustomTruststoreAppender.appendCustomTrustoreToDefault(trustStorePath, trustStorePassword);
    }
  }
}
