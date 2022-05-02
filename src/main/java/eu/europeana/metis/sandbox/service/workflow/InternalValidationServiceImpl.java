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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
class InternalValidationServiceImpl implements InternalValidationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(InternalValidationServiceImpl.class);
  private static final String SCHEMA = "EDM-INTERNAL";

  private final ValidationExecutionService validator;
  private final PatternAnalysisService<Step> patternAnalysisService;
  private final ExecutionPointService executionPointService;
  //Keep maps in memory for
  private final Map<String, LocalDateTime> datasetIdTimestampMap = new ConcurrentHashMap<>();
  private final Map<String, Lock> datasetIdLocksMap = new ConcurrentHashMap<>();
  private final Period mapEvictionPeriod = Period.ofDays(1);

  public InternalValidationServiceImpl(ValidationExecutionService validator,
      PatternAnalysisService<Step> patternAnalysisService,
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
      LOGGER.info("Pattern analysis acquiring lock record id: {}", recordToValidate.getEuropeanaId());
      generateAnalysis(recordToValidate.getDatasetId(), recordToValidate.getContent());

    } catch (PatternAnalysisException e) {
      LOGGER.error("An error occurred while processing pattern analysis with record id {}", recordToValidate.getEuropeanaId());
    }
    return new RecordInfo(recordToValidate);
  }

  private void generateAnalysis(String datasetId, byte[] recordContent) throws PatternAnalysisException {
    final Lock lock = datasetIdLocksMap.computeIfAbsent(datasetId, s -> new ReentrantLock());
    lock.lock();
    try {

      final LocalDateTime timestamp = datasetIdTimestampMap.computeIfAbsent(datasetId, s -> getLocalDateTime(datasetId));
      patternAnalysisService.generateRecordPatternAnalysis(datasetId, Step.VALIDATE_INTERNAL, timestamp,
          new String(recordContent, StandardCharsets.UTF_8));
    } finally {
      lock.unlock();
    }
  }

  private LocalDateTime getLocalDateTime(String datasetId) {
    Optional<ExecutionPoint> firstOccurrence = executionPointService.getExecutionPoint(datasetId, Step.VALIDATE_INTERNAL.toString());
    return firstOccurrence.map(ExecutionPoint::getExecutionTimestamp).orElseGet(LocalDateTime::now);
  }

  /**
   * Evict cache items every day.
   */
  @Scheduled(cron = "0 0 0 * * ?")
  private void cleanCache() {
    for (Entry<String, Lock> entry : datasetIdLocksMap.entrySet()) {
      final Lock lock = entry.getValue();
      lock.lock();
      try {
        if (datasetIdTimestampMap.get(entry.getKey()).isAfter(
            LocalDateTime.now().minus(mapEvictionPeriod))) {
          datasetIdTimestampMap.remove(entry.getKey());
          datasetIdLocksMap.remove(entry.getKey());
        }
      } finally {
        lock.unlock();
      }
    }
  }
}
