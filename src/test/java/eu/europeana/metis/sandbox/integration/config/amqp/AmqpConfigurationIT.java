package eu.europeana.metis.sandbox.integration.config.amqp;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.config.amqp.AmqpConfiguration;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.integration.testcontainers.RabbitMQTestContainersConfiguration;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
class AmqpConfigurationIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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
    RabbitMQTestContainersConfiguration.configureCustomVHost("amqpconfiguration-test");

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
    RecordProcessEvent receivedRecordProcessEvent = awaitMessage(routingKey);
    assertEquals(recordProcessEvent.getRecord().getRecordId(), receivedRecordProcessEvent.getRecord().getRecordId());
    assertEquals(recordProcessEvent.getStep(), receivedRecordProcessEvent.getStep());
    assertEquals(recordProcessEvent.getStatus(), receivedRecordProcessEvent.getStatus());
  }

  private RecordProcessEvent awaitMessage(String routingKey) {
    return (RecordProcessEvent) Awaitility.await().atMost(2, SECONDS)
                                          .until(() -> amqpTemplate.receiveAndConvert(routingKey),
                                              RecordProcessEvent.class::isInstance);
  }

  @Test
  void testRoutingToDlq() {
    //Create and start all listener container throwing exception to force the dlq routing
    AtomicInteger messagesCounter = new AtomicInteger();
    final SimpleMessageListenerContainer throwingListenerContainer = getSimpleMessageListenerContainer(messagesCounter);

    try {
      //Send a message to all queues
      amqpTemplate.convertAndSend(amqpConfiguration.getExchange(),
          amqpConfiguration.getCreatedQueue(), recordProcessEvent);
      amqpTemplate.convertAndSend(amqpConfiguration.getExchange(),
          amqpConfiguration.getExternalValidatedQueue(), recordProcessEvent);
      amqpTemplate.convertAndSend(amqpConfiguration.getExchange(),
          amqpConfiguration.getTransformedQueue(), recordProcessEvent);
      amqpTemplate.convertAndSend(amqpConfiguration.getExchange(),
          amqpConfiguration.getTransformationToEdmExternalQueue(), recordProcessEvent);
      amqpTemplate.convertAndSend(amqpConfiguration.getExchange(),
          amqpConfiguration.getInternalValidatedQueue(), recordProcessEvent);
      amqpTemplate.convertAndSend(amqpConfiguration.getExchange(),
          amqpConfiguration.getNormalizedQueue(), recordProcessEvent);
      amqpTemplate.convertAndSend(amqpConfiguration.getExchange(),
          amqpConfiguration.getEnrichedQueue(), recordProcessEvent);
      amqpTemplate.convertAndSend(amqpConfiguration.getExchange(),
          amqpConfiguration.getMediaProcessedQueue(), recordProcessEvent);
      amqpTemplate.convertAndSend(amqpConfiguration.getExchange(),
          amqpConfiguration.getPublishedQueue(), recordProcessEvent);
      amqpTemplate.convertAndSend(amqpConfiguration.getExchange(),
          amqpConfiguration.getDeBiasReadyQueue(), recordProcessEvent);

      //Await and check all dlqs
      awaitMessage(amqpConfiguration.getCreatedDlq());
      awaitMessage(amqpConfiguration.getExternalValidatedDlq());
      awaitMessage(amqpConfiguration.getTransformedDlq());
      awaitMessage(amqpConfiguration.getTransformationToEdmExternalDlq());
      awaitMessage(amqpConfiguration.getInternalValidatedDlq());
      awaitMessage(amqpConfiguration.getNormalizedDlq());
      awaitMessage(amqpConfiguration.getEnrichedDlq());
      awaitMessage(amqpConfiguration.getMediaProcessedDlq());
      awaitMessage(amqpConfiguration.getPublishedDlq());
      awaitMessage(amqpConfiguration.getDeBiasReadyDlq());
    } finally {
      //Stop if awaiting failed, so that other tests won't be impacted.
      throwingListenerContainer.stop();
    }

    assertEquals(10, messagesCounter.get());
  }

  private @NotNull SimpleMessageListenerContainer getSimpleMessageListenerContainer(AtomicInteger messagesCounter) {
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
    throwingListenerContainer.setErrorHandler(
        t -> LOGGER.warn("Forced exception to route to dlq message [{}]", messagesCounter.get()));
    throwingListenerContainer.start();
    return throwingListenerContainer;
  }
}
