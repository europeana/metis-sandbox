package eu.europeana.metis.sandbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The Spring boot application entry point.
 */
@SpringBootApplication
@EnableScheduling
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
