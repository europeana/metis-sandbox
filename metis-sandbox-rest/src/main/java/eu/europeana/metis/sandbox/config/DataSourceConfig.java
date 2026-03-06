package eu.europeana.metis.sandbox.config;

import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Configuration class for setting up the data source properties.
 *
 * <p>Defines a bean to load database connection information using the provided
 * configuration properties prefixed with "spring.datasource".
 *
 * <p>Depends on "truststoreConfig" to ensure the truststore is correctly loaded
 * before initializing the data source configuration.
 */
@Configuration
@DependsOn("truststoreConfig")
class DataSourceConfig {
  //todo - Perhaps we should change the loading of truststore to specific certificate file for postgres,
  // that would allow simpler loading of the datasource without code and just configuration properties.
  // We need to check what is the best approach for multiple dbs Postgres, Mongo, RabbitMQ, Redis.

  @Bean
  @ConfigurationProperties(prefix = "spring.datasource")
  DataSource getDataSource() {
    return DataSourceBuilder.create().build();
  }
}
