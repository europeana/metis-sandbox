package eu.europeana.metis.sandbox.config.amqp;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jayway.awaitility.Awaitility;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.test.utils.RabbitMQContainerInitializerIT;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
//Use RabbitAutoConfiguration so that the connectionFactory will connect properly to the container
@SpringBootTest(classes = {AmqpConfiguration.class, RecordMessageConverter.class, RabbitAutoConfiguration.class})
@RabbitListenerTest
public class AmqpConfigurationIT {

  @DynamicPropertySource
  public static void dynamicProperties(DynamicPropertyRegistry registry) {
    RabbitMQContainerInitializerIT.properties(registry);
  }

  private static RecordProcessEvent recordProcessEvent;
  @Autowired
  private ConnectionFactory connectionFactory;
  @Autowired
  private AmqpConfiguration amqpConfiguration;
  @Autowired
  private AmqpTemplate amqpTemplate;

  @BeforeAll
  static void beforeAll() {
    final Record recordObject = Record.builder().recordId(100L).country(Country.GREECE).language(Language.EL)
                                      .content(new byte[]{}).build();
    final RecordInfo recordInfo = new RecordInfo(recordObject);
    //Step value doesn't matter in this test
    recordProcessEvent = new RecordProcessEvent(recordInfo, Step.PUBLISH, Status.SUCCESS);
  }

  @Test
  void testQueues() {
    assertDefaultQueueSendAndReceive(amqpConfiguration.getCreatedQueue());
    assertDefaultQueueSendAndReceive(amqpConfiguration.getExternalValidatedQueue());
    assertDefaultQueueSendAndReceive(amqpConfiguration.getTransformedQueue());
    assertDefaultQueueSendAndReceive(amqpConfiguration.getTransformationToEdmExternalQueue());
    assertDefaultQueueSendAndReceive(amqpConfiguration.getInternalValidatedQueue());
    assertDefaultQueueSendAndReceive(amqpConfiguration.getNormalizedQueue());
    assertDefaultQueueSendAndReceive(amqpConfiguration.getEnrichedQueue());
    assertDefaultQueueSendAndReceive(amqpConfiguration.getMediaProcessedQueue());
    assertDefaultQueueSendAndReceive(amqpConfiguration.getPublishedQueue());

    assertDlqQueueSendAndReceive(amqpConfiguration.getCreatedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getExternalValidatedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getTransformedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getTransformationToEdmExternalDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getInternalValidatedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getNormalizedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getEnrichedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getMediaProcessedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getPublishedDlq());
  }

  private void assertDefaultQueueSendAndReceive(String routingKey) {
    assertQueueSendAndReceive(amqpConfiguration.getExchange(), routingKey);
  }

  void assertDlqQueueSendAndReceive(String routingKey) {
    assertQueueSendAndReceive(amqpConfiguration.getExchangeDlq(), routingKey);
  }

  void assertQueueSendAndReceive(String exchange, String routingKey) {
    amqpTemplate.convertAndSend(exchange, routingKey, recordProcessEvent);
    final Object receivedObject = amqpTemplate.receiveAndConvert(routingKey);
    assertTrue(receivedObject instanceof RecordProcessEvent);
    RecordProcessEvent receivedRecordProcessEvent = (RecordProcessEvent) receivedObject;
    assertEquals(recordProcessEvent.getRecord().getRecordId(), receivedRecordProcessEvent.getRecord().getRecordId());
    assertEquals(recordProcessEvent.getStep(), receivedRecordProcessEvent.getStep());
    assertEquals(recordProcessEvent.getStatus(), receivedRecordProcessEvent.getStatus());
  }

  @Test
  void testRoutingToDlq() {
    //Create and start all listener container throwing exception to force the dlq routing
    AtomicInteger messagesCounter = new AtomicInteger();
    final SimpleMessageListenerContainer throwingListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
    throwingListenerContainer.setQueueNames(
        amqpConfiguration.getCreatedQueue(),
        amqpConfiguration.getExternalValidatedQueue(),
        amqpConfiguration.getTransformedQueue(),
        amqpConfiguration.getTransformationToEdmExternalQueue(),
        amqpConfiguration.getInternalValidatedQueue(),
        amqpConfiguration.getNormalizedQueue(),
        amqpConfiguration.getEnrichedQueue(),
        amqpConfiguration.getMediaProcessedQueue(),
        amqpConfiguration.getPublishedQueue());
    throwingListenerContainer.setDefaultRequeueRejected(false);
    throwingListenerContainer.setMessageListener(message -> {
      messagesCounter.getAndIncrement();
      throw new RuntimeException("exception");
    });
    throwingListenerContainer.start();

    //Send to all queues
    amqpTemplate.convertAndSend(amqpConfiguration.getExchange(), amqpConfiguration.getCreatedQueue(), recordProcessEvent);
    amqpTemplate.convertAndSend(amqpConfiguration.getExchange(), amqpConfiguration.getExternalValidatedQueue(),
        recordProcessEvent);
    amqpTemplate.convertAndSend(amqpConfiguration.getExchange(), amqpConfiguration.getTransformedQueue(), recordProcessEvent);
    amqpTemplate.convertAndSend(amqpConfiguration.getExchange(), amqpConfiguration.getTransformationToEdmExternalQueue(),
        recordProcessEvent);
    amqpTemplate.convertAndSend(amqpConfiguration.getExchange(), amqpConfiguration.getInternalValidatedQueue(),
        recordProcessEvent);
    amqpTemplate.convertAndSend(amqpConfiguration.getExchange(), amqpConfiguration.getNormalizedQueue(), recordProcessEvent);
    amqpTemplate.convertAndSend(amqpConfiguration.getExchange(), amqpConfiguration.getEnrichedQueue(), recordProcessEvent);
    amqpTemplate.convertAndSend(amqpConfiguration.getExchange(), amqpConfiguration.getMediaProcessedQueue(), recordProcessEvent);
    amqpTemplate.convertAndSend(amqpConfiguration.getExchange(), amqpConfiguration.getPublishedQueue(), recordProcessEvent);

    //Await and check all dlqs
    awaitDqlMessages(amqpConfiguration.getCreatedDlq());
    awaitDqlMessages(amqpConfiguration.getExternalValidatedDlq());
    awaitDqlMessages(amqpConfiguration.getTransformedDlq());
    awaitDqlMessages(amqpConfiguration.getTransformationToEdmExternalDlq());
    awaitDqlMessages(amqpConfiguration.getInternalValidatedDlq());
    awaitDqlMessages(amqpConfiguration.getNormalizedDlq());
    awaitDqlMessages(amqpConfiguration.getEnrichedDlq());
    awaitDqlMessages(amqpConfiguration.getMediaProcessedDlq());
    awaitDqlMessages(amqpConfiguration.getPublishedDlq());

    throwingListenerContainer.stop();
    assertEquals(9, messagesCounter.get());
  }

  private void awaitDqlMessages(String routingKey) {
    Awaitility.await().atMost(2, SECONDS).until(() -> amqpTemplate.receiveAndConvert(routingKey) != null);
  }
}
