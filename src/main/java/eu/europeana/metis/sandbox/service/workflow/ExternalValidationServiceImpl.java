package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.exception.RecordValidationException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.record.RecordService;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.validation.service.ValidationExecutionService;
import java.io.ByteArrayInputStream;
import org.springframework.stereotype.Service;

@Service
class ExternalValidationServiceImpl implements ExternalValidationService {

  private static final String SCHEMA = "EDM-EXTERNAL";

  private final OrderingService orderingService;
  private final ValidationExecutionService validator;
  private final RecordService recordService;

  public ExternalValidationServiceImpl(
      OrderingService orderingService,
      ValidationExecutionService validationExecutionService,
      RecordService recordService) {
    this.orderingService = orderingService;
    this.validator = validationExecutionService;
    this.recordService = recordService;
  }

  @Override
  public RecordInfo validate(Record recordToValidate) {
    requireNonNull(recordToValidate, "Record must not be null");
    byte[] recordOrdered;

    try {
      recordOrdered = orderingService.performOrdering(recordToValidate.getContent());
    } catch (TransformationException e) {
      throw new RecordProcessingException(recordToValidate.getProviderId(), e);
    }

    var validationResult = validator
        .singleValidation(SCHEMA, null, null, new ByteArrayInputStream(recordOrdered));
    if (!validationResult.isSuccess()) {
      throw new RecordValidationException(validationResult.getMessage(),
          validationResult.getRecordId(), validationResult.getNodeId());
    }
    recordService.setEuropeanaIdAndProviderId(recordToValidate);
    return new RecordInfo(Record.from(recordToValidate, recordOrdered));
  }

}
