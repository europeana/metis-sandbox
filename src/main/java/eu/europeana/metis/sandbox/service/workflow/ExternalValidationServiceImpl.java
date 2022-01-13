package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.exception.RecordValidationException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.validation.service.ValidationExecutionService;
import java.io.ByteArrayInputStream;
import org.springframework.stereotype.Service;

@Service
class ExternalValidationServiceImpl implements ExternalValidationService {

  private static final String SCHEMA = "EDM-EXTERNAL";

  private final OrderingService orderingService;
  private final ValidationExecutionService validator;
  private final RecordRepository recordRepository;
  private final XmlRecordProcessorService xmlRecordProcessorService;

  public ExternalValidationServiceImpl(
      OrderingService orderingService,
      ValidationExecutionService validationExecutionService,
      RecordRepository recordRepository,
      XmlRecordProcessorService xmlRecordProcessorService) {
    this.orderingService = orderingService;
    this.validator = validationExecutionService;
    this.recordRepository = recordRepository;
    this.xmlRecordProcessorService = xmlRecordProcessorService;
  }

  @Override
  public RecordInfo validate(Record record) {
    requireNonNull(record, "Record must not be null");
    byte[] recordOrdered;
    try {
      recordOrdered = orderingService.performOrdering(record.getContent());
    } catch (TransformationException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }

    var validationResult = validator
        .singleValidation(SCHEMA, null, null, new ByteArrayInputStream(recordOrdered));
    if (!validationResult.isSuccess()) {
      throw new RecordValidationException(validationResult.getMessage(),
          validationResult.getRecordId(), validationResult.getNodeId());
    }

    setEuropeanaIdAndProviderId(record);
    return new RecordInfo(Record.from(record, recordOrdered));
  }

  private void setEuropeanaIdAndProviderId(Record record){
    String providerId = xmlRecordProcessorService.getProviderId(record.getContent());
    String europeanaId = EuropeanaIdCreator.constructEuropeanaIdString(providerId, record.getDatasetId());
    record.setEuropeanaId(europeanaId);
    record.setProviderId(providerId);
    recordRepository.updateEuropeanaIdAndProviderId(record.getRecordId(), europeanaId, providerId);
  }

}
