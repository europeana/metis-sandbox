package eu.europeana.metis.sandbox.config.amqp;

import eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AmqpConfiguration {

  @Value("${sandbox.rabbitmq.exchange.name}")
  private String exchange;

  @Value("${sandbox.rabbitmq.exchange.dlq}")
  private String exchangeDlq;

  @Value("${sandbox.rabbitmq.queues.record.created.queue}")
  private String createdQueue;

  @Value("${sandbox.rabbitmq.queues.record.created.dlq}")
  private String createdDlq;

  @Value("${sandbox.rabbitmq.queues.record.validated.external.queue}")
  private String externalValidatedQueue;

  @Value("${sandbox.rabbitmq.queues.record.validated.external.dlq}")
  private String externalValidatedDlq;

  @Value("${sandbox.rabbitmq.queues.record.transformed.queue}")
  private String transformedQueue;

  @Value("${sandbox.rabbitmq.queues.record.transformed.dlq}")
  private String transformedDlq;

  @Value("${sandbox.rabbitmq.queues.record.normalized.queue}")
  private String normalizedQueue;

  @Value("${sandbox.rabbitmq.queues.record.normalized.dlq}")
  private String normalizedDlq;

  @Value("${sandbox.rabbitmq.queues.record.validated.internal.queue}")
  private String internalValidatedQueue;

  @Value("${sandbox.rabbitmq.queues.record.validated.internal.dlq}")
  private String internalValidatedDlq;


  @Bean
  TopicExchange exchange() {
    return new TopicExchange(exchange);
  }

  @Bean
  TopicExchange dlqExchange() {
    return new TopicExchange(exchangeDlq);
  }

  @Bean
  AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
    var amqpTemplate = new RabbitTemplate(connectionFactory);
    amqpTemplate.setMessageConverter(new RecordMessageConverter());
    amqpTemplate.setExchange(exchange);
    return amqpTemplate;
  }

  @Bean
  Declarables queues() {
    return new Declarables(
        QueueBuilder.durable(createdQueue).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(
            createdDlq).build(),
        QueueBuilder.durable(externalValidatedQueue).deadLetterExchange(exchangeDlq)
            .deadLetterRoutingKey(
                externalValidatedDlq).build(),
        QueueBuilder.durable(transformedQueue).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(
            transformedDlq).build(),
        QueueBuilder.durable(normalizedQueue).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(
            normalizedDlq).build(),
        QueueBuilder.durable(internalValidatedQueue).deadLetterExchange(exchangeDlq)
            .deadLetterRoutingKey(
                internalValidatedDlq).build()
    );
  }

  @Bean
  Declarables deadQueues() {
    return new Declarables(
        QueueBuilder.durable(createdDlq).build(),
        QueueBuilder.durable(externalValidatedDlq).build(),
        QueueBuilder.durable(transformedDlq).build(),
        QueueBuilder.durable(normalizedDlq).build(),
        QueueBuilder.durable(internalValidatedDlq).build()
    );
  }

  @Bean
  Declarables bindings() {
    return getDeclarables(createdQueue, exchange, externalValidatedQueue, transformedQueue,
        normalizedQueue, internalValidatedQueue);
  }

  @Bean
  Declarables dlqBindings() {
    return getDeclarables(createdDlq, exchangeDlq, externalValidatedDlq, transformedDlq,
        normalizedDlq, internalValidatedDlq);
  }

  private Declarables getDeclarables(String createdDlq, String exchangeDlq,
      String externalValidatedDlq, String transformedDlq, String normalizedDlq,
      String internalValidatedDlq) {
    return new Declarables(
        new Binding(createdDlq, DestinationType.QUEUE, exchangeDlq, createdDlq, null),
        new Binding(externalValidatedDlq, DestinationType.QUEUE, exchangeDlq, externalValidatedDlq, null),
        new Binding(transformedDlq, DestinationType.QUEUE, exchangeDlq, transformedDlq, null),
        new Binding(normalizedDlq, DestinationType.QUEUE, exchangeDlq, normalizedDlq, null),
        new Binding(internalValidatedDlq, DestinationType.QUEUE, exchangeDlq, internalValidatedDlq, null)
    );
  }
}
