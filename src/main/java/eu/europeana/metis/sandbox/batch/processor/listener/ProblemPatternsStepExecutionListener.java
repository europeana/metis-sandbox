package eu.europeana.metis.sandbox.batch.processor.listener;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.common.ValidationBatchJobSubType;
import eu.europeana.metis.sandbox.controller.ProblemPatternAnalysisStatus;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.service.problempatterns.ExecutionPointService;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@StepScope
@Component
public class ProblemPatternsStepExecutionListener implements StepExecutionListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final PatternAnalysisService<FullBatchJobType, ExecutionPoint> patternAnalysisService;
  private final ExecutionPointService executionPointService;
  @Value("#{jobParameters['batchJobSubType']}")
  private ValidationBatchJobSubType batchJobSubType;
  @Value("#{jobParameters['datasetId']}")
  private String datasetId;

  //todo: Assuming datasetId uniqueness(therefore no lock) due to sandbox
  public ProblemPatternsStepExecutionListener(PatternAnalysisService<FullBatchJobType, ExecutionPoint> patternAnalysisService,
      ExecutionPointService executionPointService) {
    this.patternAnalysisService = patternAnalysisService;
    this.executionPointService = executionPointService;
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {
    LOGGER.info("Running beforeStep for stepName: {}, batchJobSubType: {}", stepExecution.getStepName(), batchJobSubType);
    if (batchJobSubType == ValidationBatchJobSubType.INTERNAL) {
      initializePatternAnalysisExecution();
    }
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    LOGGER.info("Running beforeStep for afterStep: {}, batchJobSubType: {}", stepExecution.getStepName(), batchJobSubType);
    if (batchJobSubType == ValidationBatchJobSubType.INTERNAL) {
      final ExecutionPoint executionPoint = executionPointService
          .getExecutionPoint(datasetId, FullBatchJobType.VALIDATE_INTERNAL.toString()).orElse(null);
      final ProblemPatternAnalysisStatus status = finalizeDatasetPatternAnalysis(datasetId, executionPoint);
      LOGGER.info("Problem patterns analysis status for datasetId {}: {}", datasetId, status);
    }
    return stepExecution.getExitStatus();
  }

  private void initializePatternAnalysisExecution() {
    final LocalDateTime timestamp = LocalDateTime.now();
    patternAnalysisService.initializePatternAnalysisExecution(datasetId, FullBatchJobType.VALIDATE_INTERNAL, timestamp);
  }

  private ProblemPatternAnalysisStatus finalizeDatasetPatternAnalysis(String datasetId, ExecutionPoint datasetExecutionPoint) {
    try {
      LOGGER.debug("Finalize analysis: {} lock, Locked", datasetId);
      patternAnalysisService.finalizeDatasetPatternAnalysis(datasetExecutionPoint);
      return ProblemPatternAnalysisStatus.FINALIZED;
    } catch (PatternAnalysisException e) {
      LOGGER.error("Something went wrong during finalizing pattern analysis", e);
      return ProblemPatternAnalysisStatus.ERROR;
    } finally {
      LOGGER.debug("Finalize analysis: {} lock, Unlocked", datasetId);
    }
  }
}
