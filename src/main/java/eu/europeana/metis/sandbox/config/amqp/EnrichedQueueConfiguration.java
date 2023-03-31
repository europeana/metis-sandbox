package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * The type Enriched queue configuration.
 */
@Configuration
public class EnrichedQueueConfiguration extends QueueConsumerConfiguration {

  @Value("${sandbox.rabbitmq.queues.record.enriched.consumers:12}")
  private int concurrentConsumers;

  @Value("${sandbox.rabbitmq.queues.record.enriched.max-consumers:12}")
  private int maxConsumers;

  @Value("${sandbox.rabbitmq.queues.record.enriched.prefetch:3}")
  private int messagePrefetchCount;

  /**
   * Instantiates a new Enriched queue configuration.
   *
   * @param messageConverter the message converter
   */
  public EnrichedQueueConfiguration(MessageConverter messageConverter) {
    super(messageConverter);
  }

  /**
   * Media processing factory simple rabbit listener container factory.
   *
   * @param configurer the configurer
   * @param connectionFactory the connection factory
   * @return the simple rabbit listener container factory
   */
  @Bean
  SimpleRabbitListenerContainerFactory mediaProcessingFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    super.setConcurrentQueueConsumers(concurrentConsumers);
    super.setMaxConcurrentQueueConsumers(maxConsumers);
    super.setMessagePrefetchCount(messagePrefetchCount);
    return super.getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }
}
