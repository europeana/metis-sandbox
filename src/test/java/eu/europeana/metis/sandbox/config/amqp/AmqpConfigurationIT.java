package eu.europeana.metis.sandbox.config.amqp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.test.utils.RabbitMQContainerInitializerIT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
//Use RabbitAutoConfiguration so that the connectionFactory will connect properly to the container
@SpringBootTest(classes = {AmqpConfiguration.class, RecordMessageConverter.class, RabbitAutoConfiguration.class})
// TODO: 17/06/2022 FIX: This now reads the application.yml from src. Needs fixing
// TODO: 17/06/2022 FIX: Check if we can test rerouting an error message from the queue to the dlq
public class AmqpConfigurationIT extends RabbitMQContainerInitializerIT {

  private static RecordProcessEvent recordProcessEvent;
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
    assertDefaultQueueSendAndReceive(amqpConfiguration.getNormalizedQueue());
    assertDefaultQueueSendAndReceive(amqpConfiguration.getInternalValidatedQueue());
    assertDefaultQueueSendAndReceive(amqpConfiguration.getEnrichedQueue());
    assertDefaultQueueSendAndReceive(amqpConfiguration.getMediaProcessedQueue());
    assertDefaultQueueSendAndReceive(amqpConfiguration.getPublishedQueue());

    assertDlqQueueSendAndReceive(amqpConfiguration.getCreatedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getExternalValidatedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getTransformedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getTransformationToEdmExternalDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getNormalizedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getInternalValidatedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getEnrichedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getMediaProcessedDlq());
    assertDlqQueueSendAndReceive(amqpConfiguration.getPublishedDlq());
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

  private void assertDefaultQueueSendAndReceive(String routingKey) {
    assertQueueSendAndReceive(amqpConfiguration.getExchange(), routingKey);
  }

  void assertDlqQueueSendAndReceive(String routingKey) {
    assertQueueSendAndReceive(amqpConfiguration.getExchangeDlq(), routingKey);
  }
}
