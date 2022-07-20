package eu.europeana.metis.sandbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SandboxApplication extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(SandboxApplication.class, args);
  }

}
