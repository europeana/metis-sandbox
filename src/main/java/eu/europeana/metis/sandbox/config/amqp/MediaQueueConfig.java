package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * The type Media queue configuration.
 */
@Configuration
public class MediaQueueConfig extends QueueConsumerConfig {

  @Value("${sandbox.rabbitmq.queues.record.media.concurrency}")
  private int concurrentConsumers;

  @Value("${sandbox.rabbitmq.queues.record.media.max-concurrency}")
  private int maxConsumers;

  @Value("${sandbox.rabbitmq.queues.record.media.prefetch}")
  private int messagePrefetchCount;

  /**
   * Instantiates a new Media queue configuration.
   *
   * @param messageConverter the message converter
   */
  public MediaQueueConfig(MessageConverter messageConverter) {
    super(messageConverter);
  }

  /**
   * Publish factory simple rabbit listener container factory.
   *
   * @param configurer the configurer
   * @param connectionFactory the connection factory
   * @return the simple rabbit listener container factory
   */
  @Bean
  SimpleRabbitListenerContainerFactory publishFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory = getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
    simpleRabbitListenerContainerFactory.setConcurrentConsumers(concurrentConsumers);
    simpleRabbitListenerContainerFactory.setMaxConcurrentConsumers(maxConsumers);
    simpleRabbitListenerContainerFactory.setPrefetchCount(messagePrefetchCount);
    return simpleRabbitListenerContainerFactory;
  }
}
