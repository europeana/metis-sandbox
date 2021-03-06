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
 * Config for record log. This binds an exchange with a queue using a routing key that listens for
 * all messages traveling through the message broker
 */
@Configuration
class RecordLogConfiguration {

  @Value("${sandbox.rabbitmq.queues.record.log.queue}")
  private String queue;

  @Value("${sandbox.rabbitmq.queues.record.log.dlq}")
  private String dlq;

  @Value("${sandbox.rabbitmq.exchange.dlq}")
  private String exchangeDlq;

  @Value("${sandbox.rabbitmq.queues.record.log.routing-key}")
  private String routingKey;

  @Value("${sandbox.rabbitmq.queues.record.log.consumers}")
  private int consumers;

  @Value("${sandbox.rabbitmq.queues.record.log.max-consumers}")
  private int maxConsumers;

  private final MessageConverter messageConverter;

  private final AmqpConfiguration amqpConfiguration;

  public RecordLogConfiguration(
      MessageConverter messageConverter,
      AmqpConfiguration amqpConfiguration) {
    this.messageConverter = messageConverter;
    this.amqpConfiguration = amqpConfiguration;
  }

  @Bean
  Queue logQueue() {
    return QueueBuilder.durable(queue).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(dlq)
        .build();
  }

  @Bean
  Queue logDlq() {
    return QueueBuilder.durable(dlq).build();
  }

  @Bean
  Binding logBinding() {
    return BindingBuilder.bind(logQueue()).to(amqpConfiguration.exchange()).with(routingKey);
  }

  @Bean
  Binding logDlqBinding() {
    return BindingBuilder.bind(logDlq()).to(amqpConfiguration.dlqExchange()).with(dlq);
  }

  // By having a factory defined for each consumer we can tune specific settings if we need to
  @Bean
  SimpleRabbitListenerContainerFactory recordLogFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    var factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setMessageConverter(messageConverter);
    factory.setConcurrentConsumers(consumers);
    factory.setMaxConcurrentConsumers(maxConsumers);
    return factory;
  }
}
