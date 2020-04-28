package eu.europeana.metis.sandbox.config;

import static org.mockito.Mockito.mock;

import eu.europeana.metis.sandbox.service.dataset.AsyncDatasetPublishService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class TestConfig {

  @Bean
  @Primary
  public AsyncDatasetPublishService publishService() {
    return mock(AsyncDatasetPublishService.class);
  }
}
