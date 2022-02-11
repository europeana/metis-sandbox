package eu.europeana.metis.sandbox.common.amqp;

import eu.europeana.metis.sandbox.domain.HarvestOaiPmhEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link MessageConverter} that can work with {@link HarvestOaiPmhEvent}
 */
//@Component
public class HarvestOaiPmhMessageConverter {
//    implements MessageConverter {

  //  protected static final int LEFT = 0;
//  protected static final int RIGHT = 1;
//  protected static final String RECORD_ID = "recordId";
//  protected static final String EUROPEANA_ID = "europeanaId";
//  protected static final String PROVIDER_ID = "providerId";
  protected static final String DATASET_ID = "datasetId";
  //  protected static final String DATASET_NAME = "datasetName";
//  protected static final String LANGUAGE = "language";
//  protected static final String COUNTRY = "country";
//  protected static final String STATUS = "status";
//  protected static final String STEP = "step";
//  protected static final String ERRORS = "errors";
  protected static final String URL = "url";
  protected static final String METADATAFORMAT = "metadataformat";
  protected static final String SET_SPEC = "set_spec";
  protected static final String OAI_RECORD_ID = "oai_record_id";
/*
  /**
   * Convert an Event to a Message.
   *
   * @param object            the object to convert
   * @param messageProperties The message properties.
   * @return the Message
   * @throws MessageConversionException in case object is not of type Event
   */
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

//  @Override
//  public Message toMessage(Object object, MessageProperties messageProperties) {
//    if (!(object instanceof HarvestOaiPmhEvent)) {
//      throw new MessageConversionException("Provided object is not of type Record");
//    }
//    HarvestOaiPmhEvent event = (HarvestOaiPmhEvent) object;
////    Record record = event.getRecord();
////    List<RecordError> errors = event.getRecordErrors();
////    logger.info("to MessageOAIPMH method: oaiRecordId {} datasetId {}", event.getOaiRecordId(),
////        event.getDatasetId());
//
//    MessageProperties properties = MessagePropertiesBuilder.newInstance()
//        .setContentType(MessageProperties.CONTENT_TYPE_XML)
//        .setHeaderIfAbsent(DATASET_ID, event.getDatasetId())
//        .setHeaderIfAbsent(OAI_RECORD_ID, event.getOaiRecordId())
//        .setHeaderIfAbsent(URL, event.getUrl())
//        .setHeaderIfAbsent(SET_SPEC, event.getSetspec())
//        .setHeaderIfAbsent(METADATAFORMAT, event.getMetadataformat())
//        .build();
//
////    if (!errors.isEmpty()) {
////      List<List<String>> errorsHeader = errors.stream().map(
////          x -> List.of(x.getMessage(), x.getStackTrace())
////      ).collect(Collectors.toList());
////      properties.setHeader(ERRORS, errorsHeader);
////    }
//
//    return MessageBuilder.withBody(new byte[0])
//        .andProperties(properties)
//        .build();
//  }

  /**
   * Convert from a Message to an Event.
   *
   * @param message the message to convert
   * @return the converted Event
   */
//  @Override
//  public Object fromMessage(Message message) {
//
//    MessageProperties properties = message.getMessageProperties();
//
//    String oaiRecordId = properties.getHeader(OAI_RECORD_ID);
//    String datasetId = properties.getHeader(DATASET_ID);
//    String url = properties.getHeader(URL);
//    String setspec = properties.getHeader(SET_SPEC);
//    String metadataformat = properties.getHeader(METADATAFORMAT);
//    logger.info("fromMessage method: oaiRecordID {} datasetID {}", oaiRecordId, datasetId);
//    return new HarvestOaiPmhEvent(url, setspec, metadataformat, oaiRecordId, datasetId);
//  }
}
