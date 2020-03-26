package eu.europeana.metis.sandbox.common.amqp;

import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.COUNTRY;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.DATASET_ID;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.DATASET_NAME;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.DEFAULT_CHARSET;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.EXCEPTION;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.LANGUAGE;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.RECORD_ID;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.STATUS;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.STEP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.support.converter.MessageConversionException;

@ExtendWith(MockitoExtension.class)
class RecordMessageConverterTest {

  @InjectMocks
  private RecordMessageConverter converter;

  @Test
  void toMessage_expectSuccess() {
    var record = Record.builder().content("This is the content").country(Country.ITALY)
        .language(Language.IT)
        .datasetId("").datasetName("").recordId("").build();
    var event = new Event(record, Step.TRANSFORM);

    var result = MessageBuilder.withBody(record.getContent().getBytes(DEFAULT_CHARSET))
        .build();

    var message = converter.toMessage(event, MessagePropertiesBuilder.newInstance().build());

    assertEquals(new String(result.getBody()), new String(message.getBody()));
  }

  @Test
  void toMessage_noEventInstance_expectFail() {
    assertThrows(MessageConversionException.class, () ->
        converter.toMessage(new Object(), MessagePropertiesBuilder.newInstance().build()));
  }

  @Test
  void fromMessage_expectSuccess() {

    MessageProperties properties = MessagePropertiesBuilder.newInstance()
        .setContentType(MessageProperties.CONTENT_TYPE_XML)
        .setHeaderIfAbsent(RECORD_ID, "")
        .setHeaderIfAbsent(DATASET_ID, "")
        .setHeaderIfAbsent(DATASET_NAME, "")
        .setHeaderIfAbsent(COUNTRY, "ITALY")
        .setHeaderIfAbsent(LANGUAGE, "IT")
        .setHeader(STEP, "TRANSFORM")
        .setHeader(STATUS, "FAIL")
        .setHeader(EXCEPTION, null)
        .build();

    var message = MessageBuilder.withBody("This is the content".getBytes(StandardCharsets.UTF_8))
        .andProperties(properties)
        .build();

    Object result = converter.fromMessage(message);

    assertThat(result, instanceOf(Event.class));
    assertEquals("This is the content", ((Event)result).getBody().getContent());
  }
}