package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class WorkflowListenerConfig {

  private final MessageConverter messageConverter;

  public WorkflowListenerConfig(
      MessageConverter messageConverter) {
    this.messageConverter = messageConverter;
  }

  @Bean
  SimpleRabbitListenerContainerFactory createdFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory externallyValidatedFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory transformedFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory internallyValidatedFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory normalizedFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory enrichedFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory mediaProcessedFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory indexedFactory(
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