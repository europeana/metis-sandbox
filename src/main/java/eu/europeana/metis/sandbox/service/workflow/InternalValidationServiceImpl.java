package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordValidationException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.service.problempatterns.ExecutionPointService;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.validation.service.ValidationExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
class InternalValidationServiceImpl implements InternalValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalValidationServiceImpl.class);
    private static final String SCHEMA = "EDM-INTERNAL";

    private final ValidationExecutionService validator;
    private final PatternAnalysisService<Step> patternAnalysisService;
    private final ExecutionPointService executionPointService;

    public InternalValidationServiceImpl(
            ValidationExecutionService validator, PatternAnalysisService<Step> patternAnalysisService,
            ExecutionPointService executionPointService) {
        this.validator = validator;
        this.patternAnalysisService = patternAnalysisService;
        this.executionPointService = executionPointService;
    }

    @Override
    public RecordInfo validate(Record recordToValidate) {
        requireNonNull(recordToValidate, "Record must not be null");

        var content = recordToValidate.getContentInputStream();
        var validationResult = validator.singleValidation(SCHEMA, null, null, content);
        if (!validationResult.isSuccess()) {
            throw new RecordValidationException(validationResult.getMessage(),
                    validationResult.getRecordId(), validationResult.getNodeId());
        }
        try {
            generateAnalysis(recordToValidate.getDatasetId(), recordToValidate.getContent());
        } catch (PatternAnalysisException e) {
            LOGGER.error("An error occurred while processing pattern analysis with record id {}", recordToValidate.getEuropeanaId());
        }
        return new RecordInfo(recordToValidate);
    }

    private void generateAnalysis(String datasetId, byte[] recordContent) throws PatternAnalysisException {
        Optional<ExecutionPoint> firstOccurrence = executionPointService.getExecutionPoint(datasetId, Step.VALIDATE_INTERNAL.toString());
        LocalDateTime timestamp = firstOccurrence.isPresent() ? firstOccurrence.get().getExecutionTimestamp() : LocalDateTime.now();
        patternAnalysisService.generateRecordPatternAnalysis(datasetId, Step.VALIDATE_INTERNAL, timestamp,
                new String(recordContent, StandardCharsets.UTF_8));
    }
}
