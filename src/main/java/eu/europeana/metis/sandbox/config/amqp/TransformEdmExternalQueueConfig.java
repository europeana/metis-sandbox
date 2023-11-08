package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The type Transform edm external queue configuration.
 */
@Configuration
public class TransformEdmExternalQueueConfig extends QueueConsumerConfig {

  @Value("${sandbox.rabbitmq.queues.record.transformation.edm.external.consumers}")
  private int concurrentConsumers;

  @Value("${sandbox.rabbitmq.queues.record.transformation.edm.external.max-consumers}")
  private int maxConsumers;

  @Value("${sandbox.rabbitmq.queues.record.transformation.edm.external.prefetch}")
  private int messagePrefetchCount;


  /**
   * Instantiates a new Transform edm external queue configuration.
   *
   * @param messageConverter the message converter
   */
  public TransformEdmExternalQueueConfig(MessageConverter messageConverter) {
    super(messageConverter);
  }


  /**
   * Transformation edm external factory simple rabbit listener container factory.
   *
   * @param configurer the configurer
   * @param connectionFactory the connection factory
   * @return the simple rabbit listener container factory
   */
  @Bean
  SimpleRabbitListenerContainerFactory transformationEdmExternalFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    setConcurrentQueueConsumers(concurrentConsumers);
    setMaxConcurrentQueueConsumers(maxConsumers);
    setMessagePrefetchCount(messagePrefetchCount);
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }
}
