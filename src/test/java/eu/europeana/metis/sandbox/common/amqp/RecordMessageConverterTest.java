package eu.europeana.metis.sandbox.common.amqp;

import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.COUNTRY;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.DATASET_ID;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.DATASET_NAME;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.ERRORS;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.EUROPEANA_ID;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.LANGUAGE;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.PROVIDER_ID;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.RECORD_ID;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.STATUS;
import static eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter.STEP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
    var record = Record.builder().content("This is the content".getBytes()).country(Country.ITALY)
        .language(Language.IT)
        .datasetId("1").datasetName("").recordId(1L).europeanaId("").build();
    var event = new RecordProcessEvent(new RecordInfo(record), "1", Step.TRANSFORM, Status.SUCCESS, 1000,
        "", "", "", null);

    var result = MessageBuilder.withBody(record.getContent())
        .build();

    var message = converter.toMessage(event, MessagePropertiesBuilder.newInstance().build());

    assertArrayEquals(result.getBody(), message.getBody());
  }

  @Test
  void toMessage_recordWithErrors_expectSuccess() {
    var record = Record.builder().content("This is the content".getBytes()).country(Country.ITALY)
        .language(Language.IT)
        .datasetId("1").datasetName("").recordId(1L).europeanaId("").build();
    var recordError = new RecordError(
        new RecordProcessingException("23", new Exception("failed here")));
    var event = new RecordProcessEvent(new RecordInfo(record, List.of(recordError)), "1", Step.TRANSFORM,
        Status.SUCCESS, 1000, "", "", "", null);

    var result = MessageBuilder.withBody(record.getContent())
        .build();

    var message = converter.toMessage(event, MessagePropertiesBuilder.newInstance().build());

    assertArrayEquals(result.getBody(), message.getBody());
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
        .setHeaderIfAbsent(RECORD_ID, 1L)
        .setHeaderIfAbsent(EUROPEANA_ID, "")
        .setHeaderIfAbsent(PROVIDER_ID, "")
        .setHeaderIfAbsent(DATASET_ID, "1")
        .setHeaderIfAbsent(DATASET_NAME, "")
        .setHeaderIfAbsent(COUNTRY, "ITALY")
        .setHeaderIfAbsent(LANGUAGE, "IT")
        .setHeader(STEP, "TRANSFORM")
        .setHeader(STATUS, "FAIL")
        .setHeader(ERRORS, null)
        .build();

    var message = MessageBuilder.withBody("This is the content".getBytes(StandardCharsets.UTF_8))
        .andProperties(properties)
        .build();

    Object result = converter.fromMessage(message);

    assertThat(result, instanceOf(RecordProcessEvent.class));
    assertArrayEquals("This is the content".getBytes(),
        ((RecordProcessEvent) result).getRecord().getContent());
  }

  @Test
  void fromMessage_recordWithErrors_expectSuccess() {

    MessageProperties properties = MessagePropertiesBuilder.newInstance()
        .setContentType(MessageProperties.CONTENT_TYPE_XML)
        .setHeaderIfAbsent(RECORD_ID, 1L)
        .setHeaderIfAbsent(EUROPEANA_ID, "")
        .setHeaderIfAbsent(PROVIDER_ID, "")
        .setHeaderIfAbsent(DATASET_ID, "1")
        .setHeaderIfAbsent(DATASET_NAME, "")
        .setHeaderIfAbsent(COUNTRY, "ITALY")
        .setHeaderIfAbsent(LANGUAGE, "IT")
        .setHeader(STEP, "TRANSFORM")
        .setHeader(STATUS, "FAIL")
        .setHeader(ERRORS, List.of(List.of("failed", "stack of failure")))
        .build();

    var message = MessageBuilder.withBody("This is the content".getBytes(StandardCharsets.UTF_8))
        .andProperties(properties)
        .build();

    Object result = converter.fromMessage(message);

    assertThat(result, instanceOf(RecordProcessEvent.class));
    assertArrayEquals("This is the content".getBytes(),
        ((RecordProcessEvent) result).getRecord().getContent());
    assertEquals("failed", ((RecordProcessEvent) result).getRecordErrors().get(0).getMessage());
  }
}