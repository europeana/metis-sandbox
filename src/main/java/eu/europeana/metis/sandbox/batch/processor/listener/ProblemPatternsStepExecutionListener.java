package eu.europeana.metis.sandbox.batch.processor.listener;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.common.ValidationBatchJobSubType;
import eu.europeana.metis.sandbox.controller.ProblemPatternAnalysisStatus;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.service.problempatterns.ExecutionPointService;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Listener that executes actions before and after a batch step to manage problem pattern analyses.
 *
 * <p>Handles initialization and finalization of pattern analysis based on the provided job parameters
 * and the step execution context.
 * <p>Applicable only when the batchJobSubType is {@link ValidationBatchJobSubType#INTERNAL}".
 */
@Slf4j
@StepScope
@Component
public class ProblemPatternsStepExecutionListener implements StepExecutionListener {

  private final PatternAnalysisService<FullBatchJobType, ExecutionPoint> patternAnalysisService;
  private final ExecutionPointService executionPointService;
  @Value("#{jobParameters['batchJobSubType']}")
  private ValidationBatchJobSubType batchJobSubType;
  @Value("#{jobParameters['datasetId']}")
  private String datasetId;

  /**
   * Constructor.
   *
   * @param patternAnalysisService The service to analyze patterns for a specific batch job type and execution point.
   * @param executionPointService The service to manage execution point entities.
   */
  //todo MET-6692: This looks fine, todo comment to be removed with ticket completion.
  public ProblemPatternsStepExecutionListener(PatternAnalysisService<FullBatchJobType, ExecutionPoint> patternAnalysisService,
      ExecutionPointService executionPointService) {
    this.patternAnalysisService = patternAnalysisService;
    this.executionPointService = executionPointService;
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {
    log.info("Running beforeStep for stepName: {}, batchJobSubType: {}", stepExecution.getStepName(), batchJobSubType);
    if (batchJobSubType == ValidationBatchJobSubType.INTERNAL) {
      initializePatternAnalysisExecution();
    }
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    log.info("Running beforeStep for afterStep: {}, batchJobSubType: {}", stepExecution.getStepName(), batchJobSubType);
    if (batchJobSubType == ValidationBatchJobSubType.INTERNAL) {
      final ExecutionPoint executionPoint = executionPointService
          .getExecutionPoint(datasetId, FullBatchJobType.VALIDATE_INTERNAL.toString()).orElse(null);
      final ProblemPatternAnalysisStatus status = finalizeDatasetPatternAnalysis(datasetId, executionPoint);
      log.info("Problem patterns analysis status for datasetId {}: {}", datasetId, status);
    }
    return stepExecution.getExitStatus();
  }

  private void initializePatternAnalysisExecution() {
    final LocalDateTime timestamp = LocalDateTime.now();
    patternAnalysisService.initializePatternAnalysisExecution(datasetId, FullBatchJobType.VALIDATE_INTERNAL, timestamp);
  }

  private ProblemPatternAnalysisStatus finalizeDatasetPatternAnalysis(String datasetId, ExecutionPoint datasetExecutionPoint) {
    try {
      log.debug("Finalize analysis: {} lock, Locked", datasetId);
      patternAnalysisService.finalizeDatasetPatternAnalysis(datasetExecutionPoint);
      return ProblemPatternAnalysisStatus.FINALIZED;
    } catch (PatternAnalysisException e) {
      log.error("Something went wrong during finalizing pattern analysis", e);
      return ProblemPatternAnalysisStatus.ERROR;
    } finally {
      log.debug("Finalize analysis: {} lock, Unlocked", datasetId);
    }
  }
}
