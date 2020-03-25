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

  @Value("${sandbox.rabbitmq.queues.record.enriched.queue}")
  private String enrichedQueue;

  @Value("${sandbox.rabbitmq.queues.record.enriched.dlq}")
  private String enrichedDlq;

  @Value("${sandbox.rabbitmq.queues.record.media.queue}")
  private String mediaProcessedQueue;

  @Value("${sandbox.rabbitmq.queues.record.media.dlq}")
  private String mediaProcessedDlq;

  @Value("${sandbox.rabbitmq.queues.record.indexed.queue}")
  private String indexedQueue;

  @Value("${sandbox.rabbitmq.queues.record.indexed.dlq}")
  private String indexedDlq;

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
        QueueBuilder.durable(externalValidatedQueue).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(externalValidatedDlq).build(),
        QueueBuilder.durable(transformedQueue).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(transformedDlq).build(),
        QueueBuilder.durable(normalizedQueue).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(normalizedDlq).build(),
        QueueBuilder.durable(internalValidatedQueue).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(internalValidatedDlq).build(),
        QueueBuilder.durable(enrichedQueue).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(enrichedDlq).build(),
        QueueBuilder.durable(mediaProcessedQueue).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(mediaProcessedDlq).build(),
        QueueBuilder.durable(indexedQueue).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(indexedDlq).build()
    );
  }

  @Bean
  Declarables deadQueues() {
    return new Declarables(
        QueueBuilder.durable(createdDlq).build(),
        QueueBuilder.durable(externalValidatedDlq).build(),
        QueueBuilder.durable(transformedDlq).build(),
        QueueBuilder.durable(normalizedDlq).build(),
        QueueBuilder.durable(internalValidatedDlq).build(),
        QueueBuilder.durable(enrichedDlq).build(),
        QueueBuilder.durable(mediaProcessedDlq).build(),
        QueueBuilder.durable(indexedDlq).build()
    );
  }

  @Bean
  Declarables bindings() {
    return getDeclarables(exchange, createdQueue, externalValidatedQueue, transformedQueue,
        normalizedQueue, internalValidatedQueue, enrichedQueue, mediaProcessedQueue, indexedQueue);
  }

  @Bean
  Declarables dlqBindings() {
    return getDeclarables(exchangeDlq, createdDlq, externalValidatedDlq, transformedDlq,
        normalizedDlq, internalValidatedDlq, enrichedDlq, mediaProcessedDlq, indexedDlq);
  }

  private Declarables getDeclarables(String exchange, String created,
      String externalValidated, String transformed, String normalized,
      String internalValidated, String enriched, String mediaProcessed, String indexed) {
    return new Declarables(
        new Binding(created, DestinationType.QUEUE, exchange, created, null),
        new Binding(externalValidated, DestinationType.QUEUE, exchange, externalValidated, null),
        new Binding(transformed, DestinationType.QUEUE, exchange, transformed, null),
        new Binding(normalized, DestinationType.QUEUE, exchange, normalized, null),
        new Binding(internalValidated, DestinationType.QUEUE, exchange, internalValidated, null),
        new Binding(enriched, DestinationType.QUEUE, exchange, enriched, null),
        new Binding(mediaProcessed, DestinationType.QUEUE, exchange, mediaProcessed, null),
        new Binding(indexed, DestinationType.QUEUE, exchange, indexed, null)
    );
  }
}
