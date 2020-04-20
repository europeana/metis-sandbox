package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.RecordValidationException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.validation.service.ValidationExecutionService;
import org.springframework.stereotype.Service;

@Service
class InternalValidationServiceImpl implements InternalValidationService {

  private static final String SCHEMA = "EDM-INTERNAL";

  private final ValidationExecutionService validator;

  public InternalValidationServiceImpl(
      ValidationExecutionService validator) {
    this.validator = validator;
  }

  @Override
  public Record validate(Record record) {
    requireNonNull(record, "Record must not be null");

    var content = record.getContentString();
    var validationResult = validator.singleValidation(SCHEMA, null, null, content);
    if (!validationResult.isSuccess()) {
      throw new RecordValidationException(validationResult.getMessage(),
          validationResult.getRecordId(), validationResult.getNodeId());
    }

    return Record.from(record, content);
  }
}
