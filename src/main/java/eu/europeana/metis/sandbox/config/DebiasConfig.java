package eu.europeana.metis.sandbox.config;


import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import eu.europeana.metis.sandbox.service.debias.CompletedState;
import eu.europeana.metis.sandbox.service.debias.DebiasMachineService;
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
    return new DebiasMachineService(detectRepository);
  }

  /**
   * Ready state
   *
   * @param debiasMachine the debias machine
   * @param detectRepository the detect repository
   * @return the ready state
   */
  @Bean
  ReadyState readyState(DetectService debiasMachine, DetectRepository detectRepository) {
    return new ReadyState(debiasMachine, detectRepository);
  }

  /**
   * Completed state
   *
   * @param debiasMachine the debias machine
   * @param detectRepository the detect repository
   * @return the completed state
   */
  @Bean
  CompletedState completedState(DetectService debiasMachine, DetectRepository detectRepository) {
    return new CompletedState(debiasMachine, detectRepository);
  }

  /**
   * Processing state
   *
   * @param debiasMachine the debias machine
   * @param detectRepository the detect repository
   * @return the processing state
   */
  @Bean
  ProcessingState processingState(DetectService debiasMachine, DetectRepository detectRepository) {
    return new ProcessingState(debiasMachine, detectRepository);
  }

  /**
   * Error state
   *
   * @param debiasMachine the debias machine
   * @param detectRepository the detect repository
   * @return the error state
   */
  @Bean
  ErrorState errorState(DetectService debiasMachine, DetectRepository detectRepository) {
    return new ErrorState(debiasMachine, detectRepository);
  }

}

