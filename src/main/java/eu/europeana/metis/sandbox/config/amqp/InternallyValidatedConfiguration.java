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
class InternallyValidatedConfiguration {

  @Value("${sandbox.rabbitmq.queues.record.validated.internal.queue}")
  private String queue;

  @Value("${sandbox.rabbitmq.queues.record.validated.internal.dlq}")
  private String dlq;

  private AmqpConfiguration amqpConfiguration;

  public InternallyValidatedConfiguration(
      AmqpConfiguration amqpConfiguration) {
    this.amqpConfiguration = amqpConfiguration;
  }

  @Bean
  Queue validatedInternalQueue() {
    return QueueBuilder.durable(queue).deadLetterExchange(dlq).build();
  }

  @Bean
  Queue validatedInternalDlq() {
    return QueueBuilder.durable(dlq).build();
  }

  @Bean
  Binding validatedInternalBinding() {
    return BindingBuilder.bind(validatedInternalQueue()).to(amqpConfiguration.exchange())
        .with(queue);
  }

  @Bean
  Binding validatedInternalDlqBinding() {
    return BindingBuilder.bind(validatedInternalDlq()).to(amqpConfiguration.dlqExchange())
        .with(dlq);
  }

  // By having a factory defined for each consumer we can tune specific settings if we need to
  @Bean
  SimpleRabbitListenerContainerFactory internallyValidatedFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    var factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setMessageConverter(new RecordMessageConverter());
    return factory;
  }
}
