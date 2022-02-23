package eu.europeana.metis.sandbox.common.amqp;

import static java.util.Objects.nonNull;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementation of {@link MessageConverter} that can work with {@link RecordProcessEvent}
 */
@Component
public class RecordMessageConverter implements MessageConverter {

  protected static final int LEFT = 0;
  protected static final int RIGHT = 1;
  protected static final String RECORD_ID = "recordId";
  protected static final String EUROPEANA_ID = "europeanaId";
  protected static final String PROVIDER_ID = "providerId";
  protected static final String DATASET_ID = "datasetId";
  protected static final String DATASET_NAME = "datasetName";
  protected static final String LANGUAGE = "language";
  protected static final String COUNTRY = "country";
  protected static final String STATUS = "status";
  protected static final String STEP = "step";
  protected static final String ERRORS = "errors";
  protected static final String URL = "url";
  protected static final String SETSPEC = "setspec";
  protected static final String METADATAFORMAT = "metadataformat";
  protected static final String MAX_RECORDS = "maxrecords";
  protected static final String XSLT_FILE = "xsltfile";

  /**
   * Convert an Event to a Message.
   *
   * @param object            the object to convert
   * @param messageProperties The message properties.
   * @return the Message
   * @throws MessageConversionException in case object is not of type RecordProcessEvent
   */
  @Override
  public Message toMessage(Object object, MessageProperties messageProperties) {
    if (!(object instanceof RecordProcessEvent)) {
      throw new MessageConversionException("Provided object is not of type Record");
    }

    RecordProcessEvent recordRecordProcessEvent = (RecordProcessEvent) object;
    Record record = recordRecordProcessEvent.getRecord();
    List<RecordError> errors = recordRecordProcessEvent.getRecordErrors();

    MessageProperties properties = MessagePropertiesBuilder.newInstance()
        .setContentType(MessageProperties.CONTENT_TYPE_XML)
        .setHeaderIfAbsent(RECORD_ID, record.getRecordId())
        .setHeaderIfAbsent(EUROPEANA_ID, record.getEuropeanaId())
        .setHeaderIfAbsent(PROVIDER_ID, record.getProviderId())
        .setHeaderIfAbsent(DATASET_ID, record.getDatasetId())
        .setHeaderIfAbsent(DATASET_NAME, record.getDatasetName())
        .setHeaderIfAbsent(COUNTRY, record.getCountry())
        .setHeaderIfAbsent(LANGUAGE, record.getLanguage())
        .setHeaderIfAbsent(STEP, recordRecordProcessEvent.getStep())
        .setHeaderIfAbsent(STATUS, recordRecordProcessEvent.getStatus())
        .setHeaderIfAbsent(URL, recordRecordProcessEvent.getUrl())
        .setHeaderIfAbsent(SETSPEC, recordRecordProcessEvent.getSetspec())
        .setHeaderIfAbsent(METADATAFORMAT, recordRecordProcessEvent.getMetadataformat())
        .setHeaderIfAbsent(MAX_RECORDS, recordRecordProcessEvent.getMaxRecords())
        .setHeaderIfAbsent(XSLT_FILE, recordRecordProcessEvent.getXsltFile())
        .build();

    if (!errors.isEmpty()) {
      List<List<String>> errorsHeader = errors.stream()
          .map(x -> List.of(x.getMessage(), x.getStackTrace())).collect(Collectors.toList());
      properties.setHeader(ERRORS, errorsHeader);
    }

    return MessageBuilder.withBody(record.getContent()).andProperties(properties).build();

  }


  /**
   * Convert from a Message to an RecordProcessEvent.
   *
   * @param message the message to convert
   * @return the converted RecordProcessEvent
   */
  @Override
  public Object fromMessage(Message message) {

    MessageProperties properties = message.getMessageProperties();
    byte[] content = message.getBody();

    Long recordId = properties.getHeader(RECORD_ID);
    String europeanaId = properties.getHeader(EUROPEANA_ID);
    String providerId = properties.getHeader(PROVIDER_ID);
    String datasetId = properties.getHeader(DATASET_ID);
    String datasetName = properties.getHeader(DATASET_NAME);
    String language = properties.getHeader(LANGUAGE);
    String country = properties.getHeader(COUNTRY);
    String step = properties.getHeader(STEP);
    String status = properties.getHeader(STATUS);
    List<List<Object>> errors = properties.getHeader(ERRORS);
    String url = properties.getHeader(URL);
    String setspec = properties.getHeader(SETSPEC);
    String metadataformat = properties.getHeader(METADATAFORMAT);
    Integer maxRecords = properties.getHeader(MAX_RECORDS);
    MultipartFile xsltFile = properties.getHeader(XSLT_FILE);

    Record record = Record.builder().recordId(recordId).europeanaId(europeanaId)
        .providerId(providerId).datasetId(datasetId).datasetName(datasetName)
        .country(Country.valueOf(country)).language(Language.valueOf(language)).content(content)
        .build();

    List<RecordError> recordErrors = List.of();

    if (nonNull(errors)) {
      recordErrors = errors.stream()
          .map(x -> new RecordError(x.get(LEFT).toString(), x.get(RIGHT).toString()))
          .collect(Collectors.toList());
    }

    RecordInfo recordInfo = new RecordInfo(record, recordErrors);

    return new RecordProcessEvent(recordInfo, datasetId, Step.valueOf(step), Status.valueOf(status),
        maxRecords, url, setspec, metadataformat, xsltFile);
  }
}
