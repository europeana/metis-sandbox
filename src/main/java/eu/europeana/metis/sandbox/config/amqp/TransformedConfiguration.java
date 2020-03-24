package eu.europeana.metis.sandbox.config.amqp;

import eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TransformedConfiguration {

  // By having a factory defined for each consumer we can tune specific settings if we need to
  @Bean
  SimpleRabbitListenerContainerFactory transformedFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    var factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setMessageConverter(new RecordMessageConverter());
    return factory;
  }
}
