package eu.europeana.metis.sandbox.config;


import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import eu.europeana.metis.sandbox.service.debias.CompletedState;
import eu.europeana.metis.sandbox.service.debias.DebiasDetectService;
import eu.europeana.metis.sandbox.service.debias.DetectService;
import eu.europeana.metis.sandbox.service.debias.ErrorState;
import eu.europeana.metis.sandbox.service.debias.ProcessingState;
import eu.europeana.metis.sandbox.service.debias.ReadyState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The type Debias config.
 */
@Configuration
public class DebiasConfig {

  /**
   * Debias machine detect service.
   *
   * @param detectRepository the detect repository
   * @return the detect service
   */
  @Bean
  public DetectService debiasMachine(DetectRepository detectRepository) {
    return new DebiasDetectService(detectRepository);
  }

}

