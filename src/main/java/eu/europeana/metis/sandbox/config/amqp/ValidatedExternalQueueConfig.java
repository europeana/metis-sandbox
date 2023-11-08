package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * The type Validated external queue configuration.
 */
@Configuration
public class ValidatedExternalQueueConfig extends QueueConsumerConfig {

  @Value("${sandbox.rabbitmq.queues.record.validated.external.consumers}")
  private int concurrentConsumers;

  @Value("${sandbox.rabbitmq.queues.record.validated.external.max-consumers}")
  private int maxConsumers;

  @Value("${sandbox.rabbitmq.queues.record.validated.external.prefetch}")
  private int messagePrefetchCount;

  /**
   * Instantiates a new Validated external queue configuration.
   *
   * @param messageConverter the message converter
   */
  public ValidatedExternalQueueConfig(MessageConverter messageConverter) {
    super(messageConverter);
  }

  /**
   * Transformation factory simple rabbit listener container factory.
   *
   * @param configurer the configurer
   * @param connectionFactory the connection factory
   * @return the simple rabbit listener container factory
   */
  @Bean
  SimpleRabbitListenerContainerFactory transformationFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    setConcurrentQueueConsumers(concurrentConsumers);
    setMaxConcurrentQueueConsumers(maxConsumers);
    setMessagePrefetchCount(messagePrefetchCount);
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }
}
