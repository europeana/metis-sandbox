package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.exception.RecordValidationException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.validation.service.ValidationExecutionService;
import java.io.ByteArrayInputStream;
import org.springframework.stereotype.Service;

@Service
class ExternalValidationServiceImpl implements ExternalValidationService {

  private static final String SCHEMA = "EDM-EXTERNAL";

  private final OrderingService orderingService;
  private final ValidationExecutionService validator;

  public ExternalValidationServiceImpl(
      OrderingService orderingService,
      ValidationExecutionService validationExecutionService) {
    this.orderingService = orderingService;
    this.validator = validationExecutionService;
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

    return new RecordInfo(Record.from(record, recordOrdered));
  }

}
