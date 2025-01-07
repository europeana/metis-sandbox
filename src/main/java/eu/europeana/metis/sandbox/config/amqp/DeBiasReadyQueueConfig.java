package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The type De bias ready queue config.
 */
@Configuration
public class DeBiasReadyQueueConfig {

  private final MessageConverter messageConverter;

  @Value("${sandbox.rabbitmq.queues.record.debias.ready.concurrency}")
  private int concurrentConsumers;

  @Value("${sandbox.rabbitmq.queues.record.debias.ready.max-concurrency}")
  private int maxConsumers;

  @Value("${sandbox.rabbitmq.queues.record.debias.ready.prefetch}")
  private int messagePrefetchCount;

  @Value("${sandbox.rabbitmq.queues.record.debias.ready.batch-size}")
  private int batchSize;

  /**
   * Instantiates a new De bias ready queue config.
   *
   * @param messageConverter the message converter
   */
  public DeBiasReadyQueueConfig(MessageConverter messageConverter) {
    this.messageConverter = messageConverter;
  }

  /**
   * Closing factory simple rabbit listener container factory.
   *
   * @param configurer the configurer
   * @param connectionFactory the connection factory
   * @return the simple rabbit listener container factory
   */
  @Bean
  SimpleRabbitListenerContainerFactory deBiasFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    var factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setConcurrentConsumers(concurrentConsumers);
    factory.setMaxConcurrentConsumers(maxConsumers);
    factory.setPrefetchCount(messagePrefetchCount);
    factory.setMessageConverter(messageConverter);
    factory.setConsumerBatchEnabled(true);
    factory.setBatchListener(true);
    factory.setBatchSize(batchSize);
    return factory;

  }
}
