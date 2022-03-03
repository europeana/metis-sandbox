package eu.europeana.metis.sandbox.config;

import static org.mockito.Mockito.mock;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import eu.europeana.indexing.Indexer;
import eu.europeana.metis.sandbox.service.dataset.AsyncRecordPublishService;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class TestConfig {

  @Bean
  ConnectionFactory connectionFactory() {
    return new CachingConnectionFactory(new MockConnectionFactory());
  }

  @Bean
  @Primary
  public AsyncRecordPublishService publishService() {
    return mock(AsyncRecordPublishService.class);
  }

  @Bean
  Indexer indexer() {
    return Mockito.mock(Indexer.class);
  }
}
