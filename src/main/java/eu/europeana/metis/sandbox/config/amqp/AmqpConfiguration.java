package eu.europeana.metis.sandbox.config.amqp;

import javax.annotation.PostConstruct;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AmqpConfiguration {

  private final MessageConverter messageConverter;

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

  private Queues queues;
  private Queues dlqs;

  public AmqpConfiguration(
      MessageConverter messageConverter) {
    this.messageConverter = messageConverter;
  }

  @PostConstruct
  public void setUp() {
    queues = new Queues()
        .setCreated(createdQueue)
        .setExternalValidated(externalValidatedQueue)
        .setTransformed(transformedQueue)
        .setInternalValidated(internalValidatedQueue)
        .setNormalized(normalizedQueue)
        .setEnriched(enrichedQueue)
        .setMediaProcessed(mediaProcessedQueue)
        .setIndexed(indexedQueue);

    dlqs = new Queues()
        .setCreated(createdDlq)
        .setExternalValidated(externalValidatedDlq)
        .setTransformed(transformedDlq)
        .setInternalValidated(internalValidatedDlq)
        .setNormalized(normalizedDlq)
        .setEnriched(enrichedDlq)
        .setMediaProcessed(mediaProcessedDlq)
        .setIndexed(indexedDlq);
  }

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
    amqpTemplate.setMessageConverter(messageConverter);
    amqpTemplate.setExchange(exchange);
    return amqpTemplate;
  }

  @Bean
  Declarables queues() {
    return new Declarables(
        QueueBuilder.durable(queues.created).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(dlqs.created).build(),
        QueueBuilder.durable(queues.externalValidated).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(dlqs.externalValidated).build(),
        QueueBuilder.durable(queues.transformed).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(dlqs.transformed).build(),
        QueueBuilder.durable(queues.normalized).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(dlqs.normalized).build(),
        QueueBuilder.durable(queues.internalValidated).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(dlqs.internalValidated).build(),
        QueueBuilder.durable(queues.enriched).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(dlqs.enriched).build(),
        QueueBuilder.durable(queues.mediaProcessed).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(dlqs.mediaProcessed).build(),
        QueueBuilder.durable(queues.indexed).deadLetterExchange(exchangeDlq).deadLetterRoutingKey(dlqs.indexed).build()
    );
  }

  @Bean
  Declarables deadQueues() {
    return new Declarables(
        QueueBuilder.durable(dlqs.created).build(),
        QueueBuilder.durable(dlqs.externalValidated).build(),
        QueueBuilder.durable(dlqs.transformed).build(),
        QueueBuilder.durable(dlqs.normalized).build(),
        QueueBuilder.durable(dlqs.internalValidated).build(),
        QueueBuilder.durable(dlqs.enriched).build(),
        QueueBuilder.durable(dlqs.mediaProcessed).build(),
        QueueBuilder.durable(dlqs.indexed).build()
    );
  }

  @Bean
  Declarables bindings() {
    return getDeclarables(queues);
  }

  @Bean
  Declarables dlqBindings() {
    return getDeclarables(dlqs);
  }

  private Declarables getDeclarables(Queues qs) {
    return new Declarables(
        new Binding(qs.created, DestinationType.QUEUE, exchange, qs.created, null),
        new Binding(qs.externalValidated, DestinationType.QUEUE, exchange, qs.externalValidated, null),
        new Binding(qs.transformed, DestinationType.QUEUE, exchange, qs.transformed, null),
        new Binding(qs.normalized, DestinationType.QUEUE, exchange, qs.normalized, null),
        new Binding(qs.internalValidated, DestinationType.QUEUE, exchange, qs.internalValidated, null),
        new Binding(qs.enriched, DestinationType.QUEUE, exchange, qs.enriched, null),
        new Binding(qs.mediaProcessed, DestinationType.QUEUE, exchange, qs.mediaProcessed, null),
        new Binding(qs.indexed, DestinationType.QUEUE, exchange, qs.indexed, null)
    );
  }

  private static class Queues {
    private String created;
    private String externalValidated;
    private String transformed;
    private String internalValidated;
    private String normalized;
    private String enriched;
    private String mediaProcessed;
    private String indexed;

    public Queues() {
      //empty constructor
    }

    public Queues setCreated(String created) {
      this.created = created;
      return this;
    }

    public Queues setExternalValidated(String externalValidated) {
      this.externalValidated = externalValidated;
      return this;
    }

    public Queues setTransformed(String transformed) {
      this.transformed = transformed;
      return this;
    }

    public Queues setInternalValidated(String internallyValidated) {
      this.internalValidated = internallyValidated;
      return this;
    }

    public Queues setNormalized(String normalized) {
      this.normalized = normalized;
      return this;
    }

    public Queues setEnriched(String enriched) {
      this.enriched = enriched;
      return this;
    }

    public Queues setMediaProcessed(String mediaProcessed) {
      this.mediaProcessed = mediaProcessed;
      return this;
    }

    public Queues setIndexed(String indexed) {
      this.indexed = indexed;
      return this;
    }
  }
}
