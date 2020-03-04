package eu.europeana.metis.sandbox.config.amqp;

import eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AmqpConfiguration {

  @Value("${sandbox.rabbitmq.exchange.name}")
  private String sandboxTopic;

  @Value("${sandbox.rabbitmq.exchange.dlq}")
  private String sandboxTopicDlq;

  @Bean
  TopicExchange exchange() {
    return new TopicExchange(sandboxTopic);
  }

  @Bean
  TopicExchange dlqExchange() {
    return new TopicExchange(sandboxTopicDlq);
  }

  @Bean
  AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
    var amqpTemplate = new RabbitTemplate(connectionFactory);
    amqpTemplate.setMessageConverter(new RecordMessageConverter());
    amqpTemplate.setExchange(sandboxTopic);
    return amqpTemplate;
  }
}
