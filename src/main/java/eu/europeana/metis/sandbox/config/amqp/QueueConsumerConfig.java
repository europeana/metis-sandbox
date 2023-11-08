package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Configuration;

/**
 * The type Queue consumer configuration for workflow listeners.
 * Every listener has a {@link SimpleRabbitListenerContainerFactory}.
 * <br /> If changes like increasing concurrency for a listener are needed,
 * every subclass can be customized by using the SimpleRabbitListenerContainerFactory.
 */
@Configuration
public abstract class QueueConsumerConfig {
  private final MessageConverter messageConverter;

  /**
   * Instantiates a new Queue consumer configuration.
   *
   * @param messageConverter the message converter
   */
  public QueueConsumerConfig(MessageConverter messageConverter) {
    this.messageConverter = messageConverter;
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
    factory.setMessageConverter(messageConverter);
    return factory;
  }
}
