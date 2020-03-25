package eu.europeana.metis.sandbox.config.amqp;

import eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TransformedConfiguration {

  @Value("${sandbox.rabbitmq.queues.record.transformed.queue}")
  private String queue;

  @Value("${sandbox.rabbitmq.queues.record.transformed.dlq}")
  private String dlq;

  @Value("${sandbox.rabbitmq.exchange.dlq}")
  private String exchangeDlq;

  private AmqpConfiguration amqpConfiguration;

  public TransformedConfiguration(
      AmqpConfiguration amqpConfiguration) {
    this.amqpConfiguration = amqpConfiguration;
  }

  @Bean
  Queue transformedQueue() {
    return QueueBuilder.durable(queue).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(dlq).build();
  }

  @Bean
  Queue transformedDlq() {
    return QueueBuilder.durable(dlq).build();
  }

  @Bean
  Binding transformedBinding() {
    return BindingBuilder.bind(transformedQueue()).to(amqpConfiguration.exchange())
        .with(queue);
  }

  @Bean
  Binding transformedDlqBinding() {
    return BindingBuilder.bind(transformedDlq()).to(amqpConfiguration.dlqExchange())
        .with(dlq);
  }

  // By having a factory defined for each consumer we can tune specific settings if we need to
  @Bean
  SimpleRabbitListenerContainerFactory transformedFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    var factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setMessageConverter(new RecordMessageConverter());
    return factory;
  }
}
