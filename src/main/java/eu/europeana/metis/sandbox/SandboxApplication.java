package eu.europeana.metis.sandbox;

import eu.europeana.metis.utils.apm.ElasticAPMConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * The Spring boot application entry point.
 */
@SpringBootApplication
@EnableConfigurationProperties({ElasticAPMConfiguration.class})
public class SandboxApplication {

  /**
   * The main spring boot method.
   *
   * @param args application arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(SandboxApplication.class, args);
  }

}
