package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.problempatterns.ExecutionPointService;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

/**
 * The type Validation workflow service.
 */
@Service
public class ValidationWorkflowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String PROVIDER = "VALIDATION-SERVICE-";
    private static final String DATASET_ID_PREFIX = "val_";
    private final ValidationStep harvestValidationStep;
    private final ValidationStep externalValidationStep;
    private final ValidationStep transformationValidationStep;
    private final ValidationStep internalValidationValidationStep;
    private final PatternAnalysisService<Step, ExecutionPoint> patternAnalysisService;
    private final ExecutionPointService executionPointService;
    private final RecordRepository recordRepository;
    private final Map<String, Lock> datasetIdLocksMap = new ConcurrentHashMap<>();
    private final LockRegistry lockRegistry;
    private final DatasetService datasetService;

    /**
     * Instantiates a new Validation workflow service.
     *
     * @param harvestValidationStep            the harvest validation step
     * @param externalValidationStep           the external validation step
     * @param transformationValidationStep     the transformation validation step
     * @param internalValidationValidationStep the internal validation validation step
     * @param datasetService                   the dataset service
     * @param recordRepository                 the record repository
     * @param patternAnalysisService           the pattern analysis service
     * @param executionPointService            the execution point service
     * @param lockRegistry                     the lock registry
     */
    public ValidationWorkflowService(ValidationStep harvestValidationStep,
                                     ValidationStep externalValidationStep,
                                     ValidationStep transformationValidationStep,
                                     ValidationStep internalValidationValidationStep,
                                     DatasetService datasetService,
                                     RecordRepository recordRepository,
                                     PatternAnalysisService<Step, ExecutionPoint> patternAnalysisService,
                                     ExecutionPointService executionPointService,
                                     LockRegistry lockRegistry) {
        this.harvestValidationStep = harvestValidationStep;
        this.externalValidationStep = externalValidationStep;
        this.transformationValidationStep = transformationValidationStep;
        this.internalValidationValidationStep = internalValidationValidationStep;
        this.patternAnalysisService = patternAnalysisService;
        this.executionPointService = executionPointService;
        this.lockRegistry = lockRegistry;
        this.datasetService = datasetService;
        this.recordRepository = recordRepository;
    }

    /**
     * Validate validation worflow report.
     *
     * @param recordToValidate the record to validate
     * @param country          the country
     * @param language         the language
     * @return the validation worflow report
     * @throws SerializationException the serialization exception
     * @throws IOException            the io exception
     */
    public ValidationWorkflowReport validate(MultipartFile recordToValidate, Country country, Language language) throws SerializationException, IOException {
        List<ValidationResult> validationResults = new ArrayList<>();
        try {
            Record harvestedRecord = createDatasetAndGetHarvestedRecord(recordToValidate, country, language);
            validationResults = performSteps(harvestedRecord);
            Optional<ExecutionPoint> datasetExecutionPointOptional = executionPointService.getExecutionPoint(harvestedRecord.getDatasetId(),
                    Step.VALIDATE_INTERNAL.toString());

            final List<ValidationResult> finalValidationResults = validationResults;
            return datasetExecutionPointOptional.flatMap(executionPoint -> {
                        finalizeValidationPatternAnalysis(harvestedRecord.getDatasetId(), executionPoint);
                        return patternAnalysisService.getDatasetPatternAnalysis(
                                harvestedRecord.getDatasetId(), Step.VALIDATE_INTERNAL, datasetExecutionPointOptional.get().getExecutionTimestamp());
                    }).map(analysis -> new ValidationWorkflowReport(finalValidationResults, analysis.getProblemPatternList()))
                    .orElseGet(() -> new ValidationWorkflowReport(finalValidationResults, List.of()));
        } catch (Exception ex) {
            LOGGER.error("Validation workflow", ex);
            validationResults.add(new ValidationResult(Step.HARVEST_FILE,
                    new RecordValidationMessage(RecordValidationMessage.Type.ERROR, ex.toString()),
                    ValidationResult.Status.FAILED));
            return new ValidationWorkflowReport(validationResults, List.of());
        }
    }

    private Record createDatasetAndGetHarvestedRecord(MultipartFile recordToValidate, Country country, Language language) throws IOException {
        final String datasetName = DATASET_ID_PREFIX + UUID.randomUUID();
        final String providerId = PROVIDER + UUID.randomUUID();
        final String datasetId = datasetService.createEmptyDataset(datasetName, country, language, null);
        datasetService.updateNumberOfTotalRecord(datasetId, 1L);
        RecordEntity recordEntity = new RecordEntity(providerId, datasetId);
        recordEntity = recordRepository.save(recordEntity);
        return new Record.RecordBuilder()
                .providerId(providerId)
                .datasetId(datasetId)
                .datasetName(datasetName)
                .country(country)
                .language(language)
                .content(recordToValidate.getInputStream().readAllBytes())
                .recordId(recordEntity.getId())
                .build();
    }

    private void finalizeValidationPatternAnalysis(String datasetId, ExecutionPoint datasetExecutionPoint) {
        final Lock lock = datasetIdLocksMap.computeIfAbsent(datasetId, s -> lockRegistry.obtain("finalizeValidationPatternAnalysis_" + datasetId));
        try {
            lock.lock();
            LOGGER.debug("Finalize record analysis: {} lock, Locked", datasetId);
            patternAnalysisService.finalizeDatasetPatternAnalysis(datasetExecutionPoint);
        } catch (PatternAnalysisException e) {
            LOGGER.error("Something went wrong during finalizing record pattern analysis", e);
        } finally {
            lock.unlock();
            LOGGER.debug("Finalize record analysis: {} lock, Unlocked", datasetId);
        }
    }

    private List<ValidationResult> performSteps(Record recordToProcess){
        List<ValidationResult> validationResults = new ArrayList<>();

        ValidationStepContent harvestResult = harvestValidationStep.performStep(recordToProcess);
        validationResults.add(harvestResult.getValidationStepResult());

        if(harvestResult.getValidationStepResult().getStatus() == ValidationResult.Status.FAILED){
            return validationResults;
        }

        ValidationStepContent externalValidationResult = externalValidationStep.performStep(harvestResult.getRecordStepResult());
        validationResults.add(externalValidationResult.getValidationStepResult());

        if(externalValidationResult.getValidationStepResult().getStatus() == ValidationResult.Status.FAILED){
            return validationResults;
        }

        ValidationStepContent transformationResult = transformationValidationStep.performStep(externalValidationResult.getRecordStepResult());
        validationResults.add(transformationResult.getValidationStepResult());

        if(transformationResult.getValidationStepResult().getStatus() == ValidationResult.Status.FAILED){
            return validationResults;
        }

        ValidationStepContent internalValidationResult = internalValidationValidationStep.performStep(transformationResult.getRecordStepResult());
        validationResults.add(internalValidationResult.getValidationStepResult());

        return validationResults;
    }
}
