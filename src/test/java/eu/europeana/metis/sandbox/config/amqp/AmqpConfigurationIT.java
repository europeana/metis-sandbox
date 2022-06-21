package eu.europeana.metis.sandbox.config.amqp;

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
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
//Use RabbitAutoConfiguration so that the connectionFactory will connect properly to the container
@SpringBootTest(classes = {AmqpConfiguration.class, RecordMessageConverter.class, RabbitAutoConfiguration.class})
// TODO: 17/06/2022 FIX: This now reads the application.yml from src. If there is no application.yml, it uses value within @Value annotation. Needs fixing
//TODO: Add @ActiveProfile (?)
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

  @Test
  void testReroutingToDlq() throws InterruptedException{
    assertReroutingIntoDlq(amqpConfiguration.getCreatedQueue(), amqpConfiguration.getCreatedDlq());
    assertReroutingIntoDlq(amqpConfiguration.getExternalValidatedQueue(), amqpConfiguration.getExternalValidatedDlq());
    assertReroutingIntoDlq(amqpConfiguration.getTransformedQueue(), amqpConfiguration.getTransformedDlq());
    assertReroutingIntoDlq(amqpConfiguration.getTransformationToEdmExternalQueue(), amqpConfiguration.getTransformationToEdmExternalDlq());
    assertReroutingIntoDlq(amqpConfiguration.getNormalizedQueue(), amqpConfiguration.getNormalizedDlq());
    assertReroutingIntoDlq(amqpConfiguration.getInternalValidatedQueue(), amqpConfiguration.getInternalValidatedDlq());
    assertReroutingIntoDlq(amqpConfiguration.getEnrichedQueue(), amqpConfiguration.getEnrichedDlq());
    assertReroutingIntoDlq(amqpConfiguration.getMediaProcessedQueue(), amqpConfiguration.getMediaProcessedDlq());
    assertReroutingIntoDlq(amqpConfiguration.getPublishedQueue(), amqpConfiguration.getPublishedDlq());

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

  void assertReroutingIntoDlq(String routingKey, String routingKeyDlq) throws InterruptedException {
    //Setting TTL with bad conversion of message to trigger putting it in dlq
    MessageProperties properties = new MessageProperties();
    properties.setExpiration("1");
    Message messageToSend = new Message(recordProcessEvent.toString().getBytes(StandardCharsets.UTF_8), properties);
    amqpTemplate.send(amqpConfiguration.getExchange(), routingKey, messageToSend);
    //Make it wait for TTL to expire
    Thread.sleep(100);
    final Message receivedMessage = amqpTemplate.receive(routingKeyDlq);
    assertNotNull(receivedMessage);
    assertArrayEquals(messageToSend.getBody(), receivedMessage.getBody());
  }
}
