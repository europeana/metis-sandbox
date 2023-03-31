package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Config for record log. This binds an exchange with a queue using a routing key that listens for all messages traveling through
 * the message broker
 */
@Configuration
class RecordLogQueueConfiguration extends QueueConsumerConfiguration {

  @Value("${sandbox.rabbitmq.queues.record.log.queue:#{null}}")
  private String queue;

  @Value("${sandbox.rabbitmq.queues.record.log.dlq:#{null}}")
  private String dlq;

  @Value("${sandbox.rabbitmq.exchange.dlq:#{null}}")
  private String exchangeDlq;

  @Value("${sandbox.rabbitmq.queues.record.log.routing-key:#{null}}")
  private String routingKey;

  @Value("${sandbox.rabbitmq.queues.record.log.consumers:12}")
  private int concurrentConsumers;

  @Value("${sandbox.rabbitmq.queues.record.log.max-consumers:12}")
  private int maxConsumers;

  @Value("${sandbox.rabbitmq.queues.record.log.prefetch:3}")
  private int messagePrefetchCount;

  private final AmqpConfiguration amqpConfiguration;

  /**
   * Instantiates a new Record log queue configuration.
   *
   * @param messageConverter the message converter
   * @param amqpConfiguration the amqp configuration
   */
  public RecordLogQueueConfiguration(MessageConverter messageConverter,
      AmqpConfiguration amqpConfiguration) {
    super(messageConverter);
    this.amqpConfiguration = amqpConfiguration;
  }

  /**
   * Log queue queue.
   *
   * @return the queue
   */
  @Bean
  Queue logQueue() {
    return QueueBuilder.durable(queue)
                       .deadLetterExchange(exchangeDlq)
                       .deadLetterRoutingKey(dlq)
                       .build();
  }

  /**
   * Log dlq queue.
   *
   * @return the queue
   */
  @Bean
  Queue logDlq() {
    return QueueBuilder.durable(dlq).build();
  }

  /**
   * Log binding binding.
   *
   * @return the binding
   */
  @Bean
  Binding logBinding() {
    return BindingBuilder.bind(logQueue())
                         .to(amqpConfiguration.exchange())
                         .with(routingKey);
  }

  /**
   * Log dlq binding binding.
   *
   * @return the binding
   */
  @Bean
  Binding logDlqBinding() {
    return BindingBuilder.bind(logDlq())
                         .to(amqpConfiguration.dlqExchange())
                         .with(dlq);
  }

  /**
   * Record log factory simple rabbit listener container factory.
   *
   * @param configurer the configurer
   * @param connectionFactory the connection factory
   * @return the simple rabbit listener container factory
   */
  @Bean
  SimpleRabbitListenerContainerFactory recordLogFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    super.setConcurrentQueueConsumers(concurrentConsumers);
    super.setMaxConcurrentQueueConsumers(maxConsumers);
    super.setMessagePrefetchCount(messagePrefetchCount);
    return super.getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }
}
