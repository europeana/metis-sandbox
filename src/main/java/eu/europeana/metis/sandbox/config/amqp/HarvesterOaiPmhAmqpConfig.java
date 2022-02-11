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
 * Config for harvester listeners. Every listener has a {@link SimpleRabbitListenerContainerFactory}.
 * <br /><br /> If changes like increasing consumers for a listener are needed, here is the place to
 * do it, by using the SimpleRabbitListenerContainerFactory
 */


@Configuration
public class HarvesterOaiPmhAmqpConfig {

  @Value("${sandbox.rabbitmq.queues.record.harvest-oai.queue}")
  private String harvestOaiPmhQueue;

  @Value("${sandbox.rabbitmq.queues.record.harvest-oai.dlq}")
  private String harvestOaiPmhDlq;

  @Value("${sandbox.rabbitmq.exchange.dlq}")
  private String exchangeDlq;

  @Value("${sandbox.rabbitmq.queues.record.harvest-oai.routing-key}")
  private String harvestOaiPmhRoutingKey;

  @Value("${sandbox.rabbitmq.queues.record.harvest-oai.consumers}")
  private int consumers;

  @Value("${sandbox.rabbitmq.queues.record.harvest-oai.max-consumers}")
  private int maxConsumers;

  private final MessageConverter messageConverter;

  private final AmqpConfiguration amqpConfiguration;

  public HarvesterOaiPmhAmqpConfig(
      MessageConverter messageConverter,
      AmqpConfiguration amqpConfiguration) {
    this.messageConverter = messageConverter;
    this.amqpConfiguration = amqpConfiguration;
  }

  @Bean
  Queue buildQueue() {
    return QueueBuilder.durable(harvestOaiPmhQueue).deadLetterExchange(exchangeDlq)
        .deadLetterRoutingKey(
            harvestOaiPmhDlq)
        .build();
  }

  @Bean
  Queue buildDlq() {
    return QueueBuilder.durable(harvestOaiPmhDlq).build();
  }

  @Bean
  Binding buildBinding() {
    return BindingBuilder.bind(buildQueue()).to(amqpConfiguration.exchange()).with(
        harvestOaiPmhRoutingKey);
  }

  @Bean
  Binding buildDlqBinding() {
    return BindingBuilder.bind(buildDlq()).to(amqpConfiguration.dlqExchange())
        .with(harvestOaiPmhDlq);
  }

  // By having a factory defined for each consumer we can tune specific settings if we need to
  @Bean
  SimpleRabbitListenerContainerFactory harvestOaiPmhFactory(
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
