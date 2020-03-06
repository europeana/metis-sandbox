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
public class ExternallyValidatedConfiguration {

  @Value("${sandbox.rabbitmq.queues.record.validated.external.queue}")
  private String queue;

  @Value("${sandbox.rabbitmq.queues.record.validated.external.dlq}")
  private String dlq;

  @Value("${sandbox.rabbitmq.exchange.dlq}")
  private String exchangeDlq;

  private AmqpConfiguration amqpConfiguration;

  public ExternallyValidatedConfiguration(
      AmqpConfiguration amqpConfiguration) {
    this.amqpConfiguration = amqpConfiguration;
  }

  @Bean
  Queue validatedExternalQueue() {
    return QueueBuilder.durable(queue).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(dlq).build();
  }

  @Bean
  Queue validatedExternalDlq() {
    return QueueBuilder.durable(dlq).build();
  }

  @Bean
  Binding validatedExternalBinding() {
    return BindingBuilder.bind(validatedExternalQueue()).to(amqpConfiguration.exchange())
        .with(queue);
  }

  @Bean
  Binding validatedExternalDlqBinding() {
    return BindingBuilder.bind(validatedExternalDlq()).to(amqpConfiguration.dlqExchange())
        .with(dlq);
  }

  // By having a factory defined for each consumer we can tune specific settings if we need to
  @Bean
  SimpleRabbitListenerContainerFactory externallyValidatedFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    var factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setMessageConverter(new RecordMessageConverter());
    return factory;
  }
}
