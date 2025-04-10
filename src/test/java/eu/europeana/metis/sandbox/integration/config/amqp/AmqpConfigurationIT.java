package eu.europeana.metis.sandbox.integration.config.amqp;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.config.amqp.AmqpConfiguration;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.test.utils.RabbitMQTestContainersConfiguration;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
//Use RabbitAutoConfiguration so that the connectionFactory will connect properly to the container
@SpringBootTest(classes = {AmqpConfiguration.class, RecordMessageConverter.class, RabbitAutoConfiguration.class})
@ActiveProfiles("amqpconfiguration-test") // Use profile to avoid interfering queue listeners from other contexts
@Import({RabbitMQTestContainersConfiguration.class})
public class AmqpConfigurationIT {

  private static RecordProcessEvent recordProcessEvent;
  @Autowired
  private ConnectionFactory connectionFactory;
  @Autowired
  private AmqpConfiguration amqpConfiguration;
  @Autowired
  private AmqpTemplate amqpTemplate;
  @Autowired
  private AmqpAdmin amqpAdmin;

  @BeforeAll
  static void beforeAll() {
    final Record recordObject = Record.builder().recordId(100L).country(Country.GREECE).language(Language.EL)
                                      .content(new byte[]{}).build();
    final RecordInfo recordInfo = new RecordInfo(recordObject);
    //Step value doesn't matter in this test
    recordProcessEvent = new RecordProcessEvent(recordInfo, Step.PUBLISH, Status.SUCCESS);
  }

  @BeforeEach
  void beforeEach() {
    amqpAdmin.purgeQueue(amqpConfiguration.getCreatedQueue());
    amqpAdmin.purgeQueue(amqpConfiguration.getExternalValidatedQueue());
    amqpAdmin.purgeQueue(amqpConfiguration.getTransformedQueue());
    amqpAdmin.purgeQueue(amqpConfiguration.getTransformationToEdmExternalQueue());
    amqpAdmin.purgeQueue(amqpConfiguration.getInternalValidatedQueue());
    amqpAdmin.purgeQueue(amqpConfiguration.getNormalizedQueue());
    amqpAdmin.purgeQueue(amqpConfiguration.getEnrichedQueue());
    amqpAdmin.purgeQueue(amqpConfiguration.getMediaProcessedQueue());
    amqpAdmin.purgeQueue(amqpConfiguration.getPublishedQueue());
    amqpAdmin.purgeQueue(amqpConfiguration.getDeBiasReadyQueue());

    amqpAdmin.purgeQueue(amqpConfiguration.getCreatedDlq());
    amqpAdmin.purgeQueue(amqpConfiguration.getExternalValidatedDlq());
    amqpAdmin.purgeQueue(amqpConfiguration.getTransformedDlq());
    amqpAdmin.purgeQueue(amqpConfiguration.getTransformationToEdmExternalDlq());
    amqpAdmin.purgeQueue(amqpConfiguration.getInternalValidatedDlq());
    amqpAdmin.purgeQueue(amqpConfiguration.getNormalizedDlq());
    amqpAdmin.purgeQueue(amqpConfiguration.getEnrichedDlq());
    amqpAdmin.purgeQueue(amqpConfiguration.getMediaProcessedDlq());
    amqpAdmin.purgeQueue(amqpConfiguration.getPublishedDlq());
    amqpAdmin.purgeQueue(amqpConfiguration.getDeBiasReadyDlq());
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
    assertDefaultQueueSendAndReceive(amqpConfiguration.getDeBiasReadyQueue());

    assertDlqQueueSendAndReceive(amqpConfiguration.getCreatedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getExternalValidatedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getTransformedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getTransformationToEdmExternalDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getInternalValidatedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getNormalizedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getEnrichedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getMediaProcessedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getPublishedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getDeBiasReadyDlq());
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
        amqpConfiguration.getPublishedQueue(),
        amqpConfiguration.getDeBiasReadyQueue());
    throwingListenerContainer.setDefaultRequeueRejected(false);
    throwingListenerContainer.setMessageListener(message -> {
      messagesCounter.getAndIncrement();
      throw new RuntimeException("exception");
    });
    throwingListenerContainer.start();

    try {
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
      amqpTemplate.convertAndSend(amqpConfiguration.getExchange(), amqpConfiguration.getMediaProcessedQueue(),
          recordProcessEvent);
      amqpTemplate.convertAndSend(amqpConfiguration.getExchange(), amqpConfiguration.getPublishedQueue(), recordProcessEvent);
      amqpTemplate.convertAndSend(amqpConfiguration.getExchange(), amqpConfiguration.getDeBiasReadyQueue(), recordProcessEvent);

      //Await and check all dlqs
      awaitDlqMessages(amqpConfiguration.getCreatedDlq());
      awaitDlqMessages(amqpConfiguration.getExternalValidatedDlq());
      awaitDlqMessages(amqpConfiguration.getTransformedDlq());
      awaitDlqMessages(amqpConfiguration.getTransformationToEdmExternalDlq());
      awaitDlqMessages(amqpConfiguration.getInternalValidatedDlq());
      awaitDlqMessages(amqpConfiguration.getNormalizedDlq());
      awaitDlqMessages(amqpConfiguration.getEnrichedDlq());
      awaitDlqMessages(amqpConfiguration.getMediaProcessedDlq());
      awaitDlqMessages(amqpConfiguration.getPublishedDlq());
      awaitDlqMessages(amqpConfiguration.getDeBiasReadyDlq());
    } finally {
      //Stop if awaiting failed, so that other tests won't be impacted.
      throwingListenerContainer.stop();
    }

    assertEquals(10, messagesCounter.get());
  }

  private void awaitDlqMessages(String routingKey) {
    Awaitility.await().atMost(2, SECONDS).until(() -> amqpTemplate.receiveAndConvert(routingKey) != null);
  }
}
