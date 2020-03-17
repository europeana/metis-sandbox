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
class CreatedConfiguration {

  @Value("${sandbox.rabbitmq.queues.record.created.queue}")
  private String queue;

  @Value("${sandbox.rabbitmq.queues.record.created.dlq}")
  private String dlq;

  private AmqpConfiguration amqpConfiguration;

  public CreatedConfiguration(
      AmqpConfiguration amqpConfiguration) {
    this.amqpConfiguration = amqpConfiguration;
  }

  @Bean
  Queue createdQueue() {
    return QueueBuilder.durable(queue).deadLetterExchange(dlq).build();
  }

  @Bean
  Queue createdDlq() {
    return QueueBuilder.durable(dlq).build();
  }

  @Bean
  Binding createdBinding() {
    return BindingBuilder.bind(createdQueue()).to(amqpConfiguration.exchange()).with(queue);
  }

  @Bean
  Binding createdDlqBinding() {
    return BindingBuilder.bind(createdDlq()).to(amqpConfiguration.dlqExchange()).with(dlq);
  }

  // By having a factory defined for each consumer we can tune specific settings if we need to
  @Bean
  SimpleRabbitListenerContainerFactory createdFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    var factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setMessageConverter(new RecordMessageConverter());
    return factory;
  }
}
