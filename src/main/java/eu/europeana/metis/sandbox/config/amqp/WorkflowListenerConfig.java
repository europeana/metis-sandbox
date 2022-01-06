package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Config for workflow listeners. Every listener has a {@link SimpleRabbitListenerContainerFactory}.
 * <br /><br />
 * If changes like increasing consumers for a listener are needed, here is the place to do it, by using the
 * SimpleRabbitListenerContainerFactory
 */
@Configuration
class WorkflowListenerConfig {

  private final MessageConverter messageConverter;

  public WorkflowListenerConfig(
      MessageConverter messageConverter) {
    this.messageConverter = messageConverter;
  }

  @Bean
  SimpleRabbitListenerContainerFactory externalValidationFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory transformationEdmExternalFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory transformationFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory internalValidationFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory normalizationFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory enrichmentFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory mediaProcessingFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory previewFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory publishFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory closingFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  private SimpleRabbitListenerContainerFactory getSimpleRabbitListenerContainerFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    var factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setMessageConverter(messageConverter);
    return factory;
  }
}
