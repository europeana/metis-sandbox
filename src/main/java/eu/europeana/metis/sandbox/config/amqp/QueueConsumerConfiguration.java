package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;

/**
 * The type Queue consumer configuration for workflow listeners.
 * Every listener has a {@link SimpleRabbitListenerContainerFactory}.
 * <br /> If changes like increasing consumers for a listener are needed,
 * every subclass can be customized by using the SimpleRabbitListenerContainerFactory
 * by default 2 consumers, 2 concurrent and 1 prefetch is set if none is defined
 * subclasses may also define their own defaults.
 */
public abstract class QueueConsumerConfiguration {

  @Value("${spring.rabbitmq.listener.simple.concurrency:2}")
  private int concurrentQueueConsumers;
  @Value("${spring.rabbitmq.listener.simple.max-concurrency:2}")
  private int maxConcurrentQueueConsumers;
  @Value("${spring.rabbitmq.listener.prefetch:1}")
  private int messagePrefetchCount;

  private final MessageConverter messageConverter;

  /**
   * Instantiates a new Queue consumer configuration.
   *
   * @param messageConverter the message converter
   */
  public QueueConsumerConfiguration(MessageConverter messageConverter) {
    this.messageConverter = messageConverter;
  }

  /**
   * Gets concurrent queue consumers.
   *
   * @return the concurrent queue consumers
   */
  public int getConcurrentQueueConsumers() {
    return concurrentQueueConsumers;
  }

  /**
   * Sets concurrent queue consumers.
   *
   * @param concurrentQueueConsumers the concurrent queue consumers
   */
  public void setConcurrentQueueConsumers(int concurrentQueueConsumers) {
    this.concurrentQueueConsumers = concurrentQueueConsumers;
  }

  /**
   * Gets max concurrent queue consumers.
   *
   * @return the max concurrent queue consumers
   */
  public int getMaxConcurrentQueueConsumers() {
    return maxConcurrentQueueConsumers;
  }

  /**
   * Sets max concurrent queue consumers.
   *
   * @param maxConcurrentQueueConsumers the max concurrent queue consumers
   */
  public void setMaxConcurrentQueueConsumers(int maxConcurrentQueueConsumers) {
    this.maxConcurrentQueueConsumers = maxConcurrentQueueConsumers;
  }

  /**
   * Gets message prefetch count.
   *
   * @return the message prefetch count
   */
  public int getMessagePrefetchCount() {
    return messagePrefetchCount;
  }

  /**
   * Sets message prefetch count.
   *
   * @param messagePrefetchCount the message prefetch count
   */
  public void setMessagePrefetchCount(int messagePrefetchCount) {
    this.messagePrefetchCount = messagePrefetchCount;
  }

  /**
   * Gets simple rabbit listener container factory.
   *
   * @param configurer the configurer
   * @param connectionFactory the connection factory
   * @return the simple rabbit listener container factory
   */
  protected SimpleRabbitListenerContainerFactory getSimpleRabbitListenerContainerFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    var factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setConcurrentConsumers(concurrentQueueConsumers);
    factory.setMaxConcurrentConsumers(maxConcurrentQueueConsumers);
    factory.setPrefetchCount(messagePrefetchCount);
    factory.setMessageConverter(messageConverter);
    return factory;
  }
}
