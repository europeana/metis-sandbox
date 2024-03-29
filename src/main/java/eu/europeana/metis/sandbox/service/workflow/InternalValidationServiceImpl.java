package eu.europeana.metis.sandbox.service.workflow;

import static java.lang.String.format;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
class InternalValidationServiceImpl implements InternalValidationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(InternalValidationServiceImpl.class);
  private static final String SCHEMA = "EDM-INTERNAL";
  private static final Period MAP_EVICTION_PERIOD = Period.ofDays(1);

  private final ValidationExecutionService validator;
  private final PatternAnalysisService<Step, ExecutionPoint> patternAnalysisService;
  private final ExecutionPointService executionPointService;
  //Keep maps in memory for unique timestamps and locking between dataset ids
  private final Map<String, LocalDateTime> datasetIdTimestampMap = new ConcurrentHashMap<>();
  private final Map<String, Lock> datasetIdLocksMap = new ConcurrentHashMap<>();
  private final LockRegistry lockRegistry;

  public InternalValidationServiceImpl(ValidationExecutionService validator,
      PatternAnalysisService<Step, ExecutionPoint> patternAnalysisService,
      ExecutionPointService executionPointService,
      LockRegistry lockRegistry) {
    this.validator = validator;
    this.patternAnalysisService = patternAnalysisService;
    this.executionPointService = executionPointService;
    this.lockRegistry = lockRegistry;
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
      LOGGER.error(format("An error occurred while processing pattern analysis with record id %s",
          recordToValidate.getEuropeanaId()), e);
    }
    return new RecordInfo(recordToValidate);
  }

  private void generateAnalysis(String datasetId, byte[] recordContent) throws PatternAnalysisException {
    final Lock lock = datasetIdLocksMap.computeIfAbsent(datasetId, s -> lockRegistry.obtain("generateAnalysis_" + datasetId));
    final ExecutionPoint executionPoint;
    try {
      lock.lock();
      LOGGER.debug("Generate analysis: {} lock, Locked", datasetId);
      final LocalDateTime timestamp = datasetIdTimestampMap.computeIfAbsent(datasetId, s -> getLocalDateTime(datasetId));
      //We have to attempt initialization everytime because we don't have an entry point for the start of the step
      executionPoint = patternAnalysisService.initializePatternAnalysisExecution(datasetId, Step.VALIDATE_INTERNAL, timestamp);
      patternAnalysisService.generateRecordPatternAnalysis(executionPoint, new String(recordContent, StandardCharsets.UTF_8));
    } finally {
      lock.unlock();
      LOGGER.debug("Generate analysis: {} lock, Unlocked", datasetId);
    }
  }

  private LocalDateTime getLocalDateTime(String datasetId) {
    //This only applies for sandbox, because the same id cannot have multiple executions.
    //In metis for example this wouldn't work if there was already a valid execution of a step in the database
    //while we are trying to run a new execution of the same step
    Optional<ExecutionPoint> firstOccurrence = executionPointService.getExecutionPoint(datasetId,
        Step.VALIDATE_INTERNAL.toString());
    return firstOccurrence.map(ExecutionPoint::getExecutionTimestamp).orElseGet(LocalDateTime::now);
  }

  /**
   * Evict cache items every day.
   */
  @Scheduled(cron = "0 0 0 * * ?")
  private void cleanCache() {
    for (Entry<String, Lock> entry : datasetIdLocksMap.entrySet()) {
      final Lock lock = entry.getValue();
      try {
        lock.lock();
        LOGGER.debug("Cleaning cache: {} lock, Locked", entry.getKey());
        if (datasetIdTimestampMap.get(entry.getKey()).isAfter(
            LocalDateTime.now().minus(MAP_EVICTION_PERIOD))) {
          datasetIdTimestampMap.remove(entry.getKey());
          datasetIdLocksMap.remove(entry.getKey());
        }
        LOGGER.debug("Dataset id maps cache cleaned");
      } finally {
        lock.unlock();
        LOGGER.debug("Cleaning cache: {} lock, Unlocked", entry.getKey());
      }
    }
  }
}
