package eu.europeana.metis.sandbox.common.amqp;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.EventError;
import eu.europeana.metis.sandbox.domain.Record;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

@Component
public class RecordMessageConverter implements MessageConverter {

  protected static final String RECORD_ID = "recordId";
  protected static final String DATASET_ID = "datasetId";
  protected static final String DATASET_NAME = "datasetName";
  protected static final String LANGUAGE = "language";
  protected static final String COUNTRY = "country";
  protected static final String STATUS = "status";
  protected static final String STEP = "step";
  protected static final String ERROR = "error";
  protected static final String STACK_TRACE = "stackTrace";

  protected static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  @Override
  public Message toMessage(Object object, MessageProperties messageProperties) {
    if (!(object instanceof Event)) {
      throw new MessageConversionException("Provided object is not of type Record");
    }

    Event recordEvent = (Event) object;
    Record record = recordEvent.getBody();
    Optional<EventError> eventError = recordEvent.getEventError();

    MessageProperties properties = MessagePropertiesBuilder.newInstance()
        .setContentType(MessageProperties.CONTENT_TYPE_XML)
        .setHeaderIfAbsent(RECORD_ID, record.getRecordId())
        .setHeaderIfAbsent(DATASET_ID, record.getDatasetId())
        .setHeaderIfAbsent(DATASET_NAME, record.getDatasetName())
        .setHeaderIfAbsent(COUNTRY, record.getCountry())
        .setHeaderIfAbsent(LANGUAGE, record.getLanguage())
        .setHeader(STEP, recordEvent.getStep())
        .setHeader(STATUS, recordEvent.getStatus())
        .build();

    if (eventError.isPresent()) {
      properties.setHeader(ERROR, eventError.get().getMessage());
      properties.setHeader(STACK_TRACE, eventError.get().getStackTrace());
    }

    return MessageBuilder.withBody(record.getContent().getBytes(DEFAULT_CHARSET))
        .andProperties(properties)
        .build();
  }

  @Override
  public Object fromMessage(Message message) {
    MessageProperties properties = message.getMessageProperties();
    String recordId = properties.getHeader(RECORD_ID);
    String datasetId = properties.getHeader(DATASET_ID);
    String datasetName = properties.getHeader(DATASET_NAME);
    String language = properties.getHeader(LANGUAGE);
    String country = properties.getHeader(COUNTRY);
    String step = properties.getHeader(STEP);
    String content = new String(message.getBody(), DEFAULT_CHARSET);
    String error = properties.getHeader(ERROR);
    Object stackTrace = properties.getHeader(STACK_TRACE);

    Record record = Record.builder()
        .recordId(recordId)
        .datasetId(datasetId)
        .datasetName(datasetName)
        .country(Country.valueOf(country))
        .language(Language.valueOf(language))
        .content(content).build();

    EventError eventError = null;
    if (error != null) {
      eventError = new EventError(error, stackTrace.toString());
    }

    return new Event(record, Step.valueOf(step), eventError);
  }
}
