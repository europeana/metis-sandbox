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
class MediaProcessedConfiguration {

  @Value("${sandbox.rabbitmq.queues.record.media.queue}")
  private String queue;

  @Value("${sandbox.rabbitmq.queues.record.media.dlq}")
  private String dlq;

  private AmqpConfiguration amqpConfiguration;

  public MediaProcessedConfiguration(
      AmqpConfiguration amqpConfiguration) {
    this.amqpConfiguration = amqpConfiguration;
  }

  @Bean
  Queue mediaQueue() {
    return QueueBuilder.durable(queue).deadLetterExchange(dlq).build();
  }

  @Bean
  Queue mediaDlq() {
    return QueueBuilder.durable(dlq).build();
  }

  @Bean
  Binding mediaBinding() {
    return BindingBuilder.bind(mediaQueue()).to(amqpConfiguration.exchange())
        .with(queue);
  }

  @Bean
  Binding mediaDlqBinding() {
    return BindingBuilder.bind(mediaDlq()).to(amqpConfiguration.dlqExchange())
        .with(dlq);
  }

  // By having a factory defined for each consumer we can tune specific settings if we need to
  @Bean
  SimpleRabbitListenerContainerFactory mediaProcessedFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    var factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setMessageConverter(new RecordMessageConverter());
    return factory;
  }
}
