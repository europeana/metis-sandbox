package eu.europeana.metis.sandbox.common.amqp;

import com.rabbitmq.client.LongString;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

public class RecordMessageConverter implements MessageConverter {

  private static final String RECORD_ID = "recordId";
  private static final String DATASET_ID = "datasetId";
  private static final String DATASET_NAME = "datasetName";
  private static final String LANGUAGE = "language";
  private static final String COUNTRY = "country";
  private static final String STATUS = "status";
  private static final String STEP = "step";
  private static final String EXCEPTION = "exception";

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  @Override
  public Message toMessage(Object object, MessageProperties messageProperties)
      throws MessageConversionException {
    if (!(object instanceof Event)) {
      throw new MessageConversionException("Provided object is not of type Record");
    }

    Event<Record> recordEvent = (Event<Record>) object;
    Record record = recordEvent.getBody();

    MessageProperties properties = MessagePropertiesBuilder.newInstance()
        .setContentType(MessageProperties.CONTENT_TYPE_XML)
        .setHeaderIfAbsent(RECORD_ID, record.getRecordId())
        .setHeaderIfAbsent(DATASET_ID, record.getDatasetId())
        .setHeaderIfAbsent(DATASET_NAME, record.getDatasetName())
        .setHeaderIfAbsent(COUNTRY, record.getCountry())
        .setHeaderIfAbsent(LANGUAGE, record.getLanguage())
        .setHeader(STEP, recordEvent.getStep())
        .setHeader(STATUS, recordEvent.getStatus())
        .setHeader(EXCEPTION, recordEvent.getException())
        .build();

    return MessageBuilder.withBody(record.getContent().getBytes(DEFAULT_CHARSET))
        .andProperties(properties)
        .build();
  }

  @Override
  public Object fromMessage(Message message) throws MessageConversionException {
    MessageProperties properties = message.getMessageProperties();
    String recordId = properties.getHeader(RECORD_ID);
    String datasetId = properties.getHeader(DATASET_ID);
    String datasetName = properties.getHeader(DATASET_NAME);
    String language = properties.getHeader(LANGUAGE);
    String country = properties.getHeader(COUNTRY);
    String step = properties.getHeader(STEP);
    String content = new String(message.getBody(), DEFAULT_CHARSET);
    LongString exceptionObject = properties.getHeader(EXCEPTION);
    String exception = exceptionObject == null ? null : exceptionObject.toString();

    Record record = Record.builder()
        .recordId(recordId)
        .datasetId(datasetId)
        .datasetName(datasetName)
        .country(Country.valueOf(country))
        .language(Language.valueOf(language))
        .content(content).build();

    return new Event<>(record, Step.valueOf(step), exception);
  }
}
