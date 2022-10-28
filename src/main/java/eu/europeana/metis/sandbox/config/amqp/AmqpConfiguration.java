package eu.europeana.metis.sandbox.config.amqp;

import javax.annotation.PostConstruct;
import org.springframework.amqp.core.AmqpAdmin;
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

import java.util.List;

/**
 * Config for amqp, contains exchange, queues and dead letter queues definitions as well as their bindings. <br /><br /> If there
 * is a need to add a new queue in the future here is the place to do it
 */
@Configuration
public class AmqpConfiguration {

  private final MessageConverter messageConverter;
  private final AmqpAdmin amqpAdmin;

  @Value("${sandbox.rabbitmq.exchange.name:#{null}}")
  private String exchange;

  @Value("${sandbox.rabbitmq.exchange.dlq:#{null}}")
  private String exchangeDlq;

  @Value("${sandbox.rabbitmq.queues.record.created.queue:#{null}}")
  private String createdQueue;

  @Value("${sandbox.rabbitmq.queues.record.created.dlq:#{null}}")
  private String createdDlq;

  @Value("${sandbox.rabbitmq.queues.record.transformation.edm.external.queue:#{null}}")
  private String transformationToEdmExternalQueue;

  @Value("${sandbox.rabbitmq.queues.record.transformation.edm.external.dlq:#{null}}")
  private String transformationToEdmExternalDlq;

  @Value("${sandbox.rabbitmq.queues.record.validated.external.queue:#{null}}")
  private String externalValidatedQueue;

  @Value("${sandbox.rabbitmq.queues.record.validated.external.dlq:#{null}}")
  private String externalValidatedDlq;

  @Value("${sandbox.rabbitmq.queues.record.transformed.queue:#{null}}")
  private String transformedQueue;

  @Value("${sandbox.rabbitmq.queues.record.transformed.dlq:#{null}}")
  private String transformedDlq;

  @Value("${sandbox.rabbitmq.queues.record.normalized.queue:#{null}}")
  private String normalizedQueue;

  @Value("${sandbox.rabbitmq.queues.record.normalized.dlq:#{null}}")
  private String normalizedDlq;

  @Value("${sandbox.rabbitmq.queues.record.validated.internal.queue:#{null}}")
  private String internalValidatedQueue;

  @Value("${sandbox.rabbitmq.queues.record.validated.internal.dlq:#{null}}")
  private String internalValidatedDlq;

  @Value("${sandbox.rabbitmq.queues.record.enriched.queue:#{null}}")
  private String enrichedQueue;

  @Value("${sandbox.rabbitmq.queues.record.enriched.dlq:#{null}}")
  private String enrichedDlq;

  @Value("${sandbox.rabbitmq.queues.record.media.queue:#{null}}")
  private String mediaProcessedQueue;

  @Value("${sandbox.rabbitmq.queues.record.media.dlq:#{null}}")
  private String mediaProcessedDlq;

  @Value("${sandbox.rabbitmq.queues.record.published.queue:#{null}}")
  private String publishedQueue;

  @Value("${sandbox.rabbitmq.queues.record.published.dlq:#{null}}")
  private String publishedDlq;


  public AmqpConfiguration(MessageConverter messageConverter, AmqpAdmin amqpAdmin) {
    this.messageConverter = messageConverter;
    this.amqpAdmin = amqpAdmin;
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
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(messageConverter);
    rabbitTemplate.setExchange(exchange);
    return rabbitTemplate;
  }

  @Bean
  Declarables deadQueues() {
    return new Declarables(
        QueueBuilder.durable(createdDlq).build(),
        QueueBuilder.durable(transformationToEdmExternalDlq).build(),
        QueueBuilder.durable(externalValidatedDlq).build(),
        QueueBuilder.durable(transformedDlq).build(),
        QueueBuilder.durable(normalizedDlq).build(),
        QueueBuilder.durable(internalValidatedDlq).build(),
        QueueBuilder.durable(enrichedDlq).build(),
        QueueBuilder.durable(mediaProcessedDlq).build(),
        QueueBuilder.durable(publishedDlq).build()
    );
  }

  @Bean
  Declarables bindings() {
    return getDeclarables(exchange, createdQueue, transformationToEdmExternalQueue,
        externalValidatedQueue, transformedQueue, normalizedQueue, internalValidatedQueue,
        enrichedQueue, mediaProcessedQueue, publishedQueue);
  }

  @Bean
  Declarables dlqBindings() {
    return getDeclarables(exchangeDlq, createdDlq, transformationToEdmExternalDlq,
        externalValidatedDlq, transformedDlq, normalizedDlq, internalValidatedDlq, enrichedDlq,
        mediaProcessedDlq, publishedDlq);
  }

  //Suppress: Methods should not have too many parameters warning
  //We are okay with this method to ease configuration

  @SuppressWarnings("squid:S107")
  private Declarables getDeclarables(String exchange, String created,
      String transformationToEdmExternal, String externalValidated, String transformed,
      String normalized, String internalValidated, String enriched, String mediaProcessed,
      String published) {
    return new Declarables(
        new Binding(created, DestinationType.QUEUE, exchange, created, null),
        new Binding(transformationToEdmExternal, DestinationType.QUEUE, exchange,
            transformationToEdmExternal, null),
        new Binding(externalValidated, DestinationType.QUEUE, exchange, externalValidated, null),
        new Binding(transformed, DestinationType.QUEUE, exchange, transformed, null),
        new Binding(normalized, DestinationType.QUEUE, exchange, normalized, null),
        new Binding(internalValidated, DestinationType.QUEUE, exchange, internalValidated, null),
        new Binding(enriched, DestinationType.QUEUE, exchange, enriched, null),
        new Binding(mediaProcessed, DestinationType.QUEUE, exchange, mediaProcessed, null),
        new Binding(published, DestinationType.QUEUE, exchange, published, null)
    );
  }

  @Bean
  Declarables queues() {
    return new Declarables(
        QueueBuilder.durable(createdQueue).deadLetterExchange(exchangeDlq)
                    .deadLetterRoutingKey(createdDlq).build(),
        QueueBuilder.durable(transformationToEdmExternalQueue).deadLetterExchange(exchangeDlq)
                    .deadLetterRoutingKey(
                        transformationToEdmExternalDlq).build(),
        QueueBuilder.durable(externalValidatedQueue).deadLetterExchange(exchangeDlq)
                    .deadLetterRoutingKey(externalValidatedDlq).build(),
        QueueBuilder.durable(transformedQueue).deadLetterExchange(exchangeDlq)
                    .deadLetterRoutingKey(transformedDlq).build(),
        QueueBuilder.durable(normalizedQueue).deadLetterExchange(exchangeDlq)
                    .deadLetterRoutingKey(normalizedDlq).build(),
        QueueBuilder.durable(internalValidatedQueue).deadLetterExchange(exchangeDlq)
                    .deadLetterRoutingKey(internalValidatedDlq).build(),
        QueueBuilder.durable(enrichedQueue).deadLetterExchange(exchangeDlq)
                    .deadLetterRoutingKey(enrichedDlq).build(),
        QueueBuilder.durable(mediaProcessedQueue).deadLetterExchange(exchangeDlq)
                    .deadLetterRoutingKey(mediaProcessedDlq).build(),
        QueueBuilder.durable(publishedQueue).deadLetterExchange(exchangeDlq)
                    .deadLetterRoutingKey(publishedDlq).build()
    );
  }

  @PostConstruct
  void postConstruct() {
    //This will create the exchanges/queues on startup instead of waiting for first connection request
    amqpAdmin.initialize();
  }

  public String getExchange() {
    return exchange;
  }

  public String getExchangeDlq() {
    return exchangeDlq;
  }

  public String getCreatedQueue() {
    return createdQueue;
  }

  public String getCreatedDlq() {
    return createdDlq;
  }

  public String getTransformationToEdmExternalQueue() {
    return transformationToEdmExternalQueue;
  }

  public String getTransformationToEdmExternalDlq() {
    return transformationToEdmExternalDlq;
  }

  public String getExternalValidatedQueue() {
    return externalValidatedQueue;
  }

  public String getExternalValidatedDlq() {
    return externalValidatedDlq;
  }

  public String getTransformedQueue() {
    return transformedQueue;
  }

  public String getTransformedDlq() {
    return transformedDlq;
  }

  public String getNormalizedQueue() {
    return normalizedQueue;
  }

  public String getNormalizedDlq() {
    return normalizedDlq;
  }

  public String getInternalValidatedQueue() {
    return internalValidatedQueue;
  }

  public String getInternalValidatedDlq() {
    return internalValidatedDlq;
  }

  public String getEnrichedQueue() {
    return enrichedQueue;
  }

  public String getEnrichedDlq() {
    return enrichedDlq;
  }

  public String getMediaProcessedQueue() {
    return mediaProcessedQueue;
  }

  public String getMediaProcessedDlq() {
    return mediaProcessedDlq;
  }

  public String getPublishedQueue() {
    return publishedQueue;
  }

  public String getPublishedDlq() {
    return publishedDlq;
  }

  public AmqpAdmin getAmqpAdmin(){
    return amqpAdmin;
  }

  public List<String> getQueuesNames(){
    return List.of(createdQueue, transformationToEdmExternalQueue, externalValidatedQueue, transformedQueue, internalValidatedQueue,
            normalizedQueue, enrichedQueue, mediaProcessedQueue, publishedQueue);
  }

  public List<String> getDlqQueuesNames(){
    return List.of(createdDlq, transformationToEdmExternalDlq, externalValidatedDlq, transformedDlq, internalValidatedDlq,
            normalizedDlq, enrichedDlq, mediaProcessedDlq, publishedDlq);
  }
}
