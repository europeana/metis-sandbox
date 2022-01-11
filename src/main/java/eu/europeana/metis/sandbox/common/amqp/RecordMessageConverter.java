package eu.europeana.metis.sandbox.common.amqp;

import static java.util.Objects.nonNull;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link MessageConverter} that can work with {@link Event}
 */
@Component
class RecordMessageConverter implements MessageConverter {

  protected static final int LEFT = 0;
  protected static final int RIGHT = 1;
  protected static final String RECORD_ID = "recordId";
  protected static final String EUROPEANA_ID = "europeanaId";
  protected static final String DATASET_ID = "datasetId";
  protected static final String DATASET_NAME = "datasetName";
  protected static final String LANGUAGE = "language";
  protected static final String COUNTRY = "country";
  protected static final String STATUS = "status";
  protected static final String STEP = "step";
  protected static final String ERRORS = "errors";

  /**
   * Convert an Event to a Message.
   *
   * @param object            the object to convert
   * @param messageProperties The message properties.
   * @return the Message
   * @throws MessageConversionException in case object is not of type Event
   */
  @Override
  public Message toMessage(Object object, MessageProperties messageProperties) {
    if (!(object instanceof Event)) {
      throw new MessageConversionException("Provided object is not of type Record");
    }

    Event recordEvent = (Event) object;
    Record record = recordEvent.getBody();
    List<RecordError> errors = recordEvent.getRecordErrors();

    MessageProperties properties = MessagePropertiesBuilder.newInstance()
        .setContentType(MessageProperties.CONTENT_TYPE_XML)
        .setHeaderIfAbsent(RECORD_ID, record.getRecordId())
        .setHeaderIfAbsent(EUROPEANA_ID, record.getEuropeanaId())
        .setHeaderIfAbsent(DATASET_ID, record.getDatasetId())
        .setHeaderIfAbsent(DATASET_NAME, record.getDatasetName())
        .setHeaderIfAbsent(COUNTRY, record.getCountry())
        .setHeaderIfAbsent(LANGUAGE, record.getLanguage())
        .setHeader(STEP, recordEvent.getStep())
        .setHeader(STATUS, recordEvent.getStatus())
        .build();

    if (!errors.isEmpty()) {
      List<List<String>> errorsHeader = errors.stream().map(
          x -> List.of(x.getMessage(), x.getStackTrace())
      ).collect(Collectors.toList());
      properties.setHeader(ERRORS, errorsHeader);
    }

    return MessageBuilder.withBody(record.getContent())
        .andProperties(properties)
        .build();
  }

  /**
   * Convert from a Message to an Event.
   *
   * @param message the message to convert
   * @return the converted Event
   */
  @Override
  public Object fromMessage(Message message) {
    byte[] content = message.getBody();
    MessageProperties properties = message.getMessageProperties();
    String recordId = properties.getHeader(RECORD_ID);
    String europeanaId = properties.getHeader(EUROPEANA_ID);
    String datasetId = properties.getHeader(DATASET_ID);
    String datasetName = properties.getHeader(DATASET_NAME);
    String language = properties.getHeader(LANGUAGE);
    String country = properties.getHeader(COUNTRY);
    String step = properties.getHeader(STEP);
    String status = properties.getHeader(STATUS);
    List<List<Object>> errors = properties.getHeader(ERRORS);

    Record record = Record.builder()
        .recordId(recordId)
        .europeanaId(europeanaId)
        .datasetId(datasetId)
        .datasetName(datasetName)
        .country(Country.valueOf(country))
        .language(Language.valueOf(language))
        .content(content).build();

    List<RecordError> recordErrors = List.of();

    if (nonNull(errors)) {
      recordErrors = errors.stream()
          .map(x -> new RecordError(x.get(LEFT).toString(), x.get(RIGHT).toString()))
          .collect(Collectors.toList());
    }

    RecordInfo recordInfo = new RecordInfo(record, recordErrors);

    return new Event(recordInfo, Step.valueOf(step), Status.valueOf(status));
  }
}
