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

@Configuration
public class DebiasConfig {

  @Bean
  public DetectService debiasMachine(DetectRepository detectRepository) {
    return new DebiasMachineService(detectRepository);
  }

  @Bean
  ReadyState readyState(DetectService debiasMachine, DetectRepository detectRepository) {
    return new ReadyState(debiasMachine, detectRepository);
  }

  @Bean
  CompletedState completedState(DetectService debiasMachine, DetectRepository detectRepository) {
    return new CompletedState(debiasMachine, detectRepository);
  }

  @Bean
  ProcessingState processingState(DetectService debiasMachine, DetectRepository detectRepository) {
    return new ProcessingState(debiasMachine, detectRepository);
  }

  @Bean
  ErrorState errorState(DetectService debiasMachine, DetectRepository detectRepository) {
    return new ErrorState(debiasMachine, detectRepository);
  }

}

