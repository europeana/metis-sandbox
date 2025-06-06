package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.common.ValidationBatchJobSubType;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.validation.model.ValidationResult;
import eu.europeana.validation.service.ValidationExecutionService;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Service;

@Service
public class ValidationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final ValidationExecutionService validationExecutionService;
  private final ObjectFactory<XsltTransformer> xsltTransformerFactory;
  private final PatternAnalysisService<FullBatchJobType, ExecutionPoint> patternAnalysisService;
  private final ExecutionPointRepository executionPointRepository;

  public ValidationService(
      ValidationExecutionService validationExecutionService,
      ObjectFactory<XsltTransformer> xsltTransformerFactory,
      PatternAnalysisService<FullBatchJobType, ExecutionPoint> patternAnalysisService,
      ExecutionPointRepository executionPointRepository) {
    this.validationExecutionService = validationExecutionService;
    this.xsltTransformerFactory = xsltTransformerFactory;
    this.patternAnalysisService = patternAnalysisService;
    this.executionPointRepository = executionPointRepository;
  }

  public ValidationResult validateRecord(String recordData, String datasetId, String executionName, ValidationBatchJobSubType subtype)
      throws TransformationException {
      String schema = switch (subtype) {
        case EXTERNAL -> "EDM-EXTERNAL";
        case INTERNAL -> "EDM-INTERNAL";
      };

      String reorderedFileContent = reorderFileContent(recordData);
      ValidationResult result = validationExecutionService.singleValidation(schema, null, null, reorderedFileContent);

      if (subtype == ValidationBatchJobSubType.INTERNAL) {
        generatePatternAnalysis(datasetId, executionName, reorderedFileContent);
      }

      return result;
  }

  private String reorderFileContent(String recordData) throws TransformationException {
    try (XsltTransformer xsltTransformer = xsltTransformerFactory.getObject()) {
      StringWriter writer = xsltTransformer.transform(recordData.getBytes(StandardCharsets.UTF_8), null);
      return writer.toString();
    }
  }

  private void generatePatternAnalysis(String datasetId, String executionName, String reorderedRecordData) {
    Optional<ExecutionPoint> executionPoint = executionPointRepository.findFirstByDatasetIdAndExecutionNameOrderByExecutionTimestampDesc(
        datasetId, executionName);
    if (executionPoint.isEmpty()) {
      throw new IllegalStateException("No execution point found for datasetId " + datasetId);
    }
    try {
      patternAnalysisService.generateRecordPatternAnalysis(executionPoint.get(), reorderedRecordData);
    } catch (PatternAnalysisException e) {
      LOGGER.error("An error occurred while processing pattern analysis", e);
    }
  }
}

