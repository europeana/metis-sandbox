package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordValidationException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.validation.service.ValidationExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
class InternalValidationServiceImpl implements InternalValidationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(InternalValidationServiceImpl.class);

  private static final String SCHEMA = "EDM-INTERNAL";

  private final ValidationExecutionService validator;
  private final PatternAnalysisService<Step> patternAnalysisService;

  public InternalValidationServiceImpl(
      ValidationExecutionService validator, PatternAnalysisService<Step> patternAnalysisService) {
    this.validator = validator;
    this.patternAnalysisService = patternAnalysisService;
  }

  @Override
  public RecordInfo validate(Record recordToValidate, LocalDateTime timestamp) {
    requireNonNull(recordToValidate, "Record must not be null");

    var content = recordToValidate.getContentInputStream();
    var validationResult = validator.singleValidation(SCHEMA, null, null, content);
    if (!validationResult.isSuccess()) {
      throw new RecordValidationException(validationResult.getMessage(),
          validationResult.getRecordId(), validationResult.getNodeId());
    }
    try {
      patternAnalysisService.generateRecordPatternAnalysis(recordToValidate.getDatasetId(), Step.VALIDATE_INTERNAL, timestamp,
              new String(recordToValidate.getContent(), StandardCharsets.UTF_8));
    } catch (PatternAnalysisException e) {
      LOGGER.error(String.format("An error occurred while processing pattern analysis with record id %s", recordToValidate.getEuropeanaId()));
    }
    return new RecordInfo(recordToValidate);
  }
}
