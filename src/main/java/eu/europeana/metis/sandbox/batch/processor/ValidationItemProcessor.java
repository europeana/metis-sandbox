package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;
import static java.lang.String.format;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordAndDTOConverterUtil;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.common.ValidationBatchJobSubType;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.validation.model.ValidationResult;
import eu.europeana.validation.service.ValidationExecutionService;
import jakarta.annotation.PostConstruct;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.experimental.StandardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@StepScope
@Component("validationItemProcessor")
public class ValidationItemProcessor extends AbstractMetisItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ValidationExecutionService validationExecutionService;
  private final ObjectFactory<XsltTransformer> xsltTransformerFactory;
  private final ItemProcessorUtil itemProcessorUtil;
  private final PatternAnalysisService<FullBatchJobType, ExecutionPoint> patternAnalysisService;
  private final ExecutionPointRepository executionPointRepository;
  private String schema;

  public ValidationItemProcessor(ValidationExecutionService validationExecutionService,
      ObjectFactory<XsltTransformer> xsltTransformerFactory,
      PatternAnalysisService<FullBatchJobType, ExecutionPoint> patternAnalysisService,
      ExecutionPointRepository executionPointRepository) {
    this.validationExecutionService = validationExecutionService;
    this.xsltTransformerFactory = xsltTransformerFactory;
    this.patternAnalysisService = patternAnalysisService;
    this.executionPointRepository = executionPointRepository;
    itemProcessorUtil = new ItemProcessorUtil(getProcessRecordFunction());
  }

  @Override
  public ExecutionRecordDTO process(@NonNull ExecutionRecord executionRecord) {
    final SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = ExecutionRecordAndDTOConverterUtil.converterToExecutionRecordDTO(
        executionRecord);
    JobMetadataDTO jobMetadataDTO = new JobMetadataDTO(originSuccessExecutionRecordDTO, getExecutionName(),
        getTargetExecutionId());
    ExecutionRecordDTO resultExecutionRecordDTO = itemProcessorUtil.processCapturingException(jobMetadataDTO);
    if (getFullBatchJobType().getBatchJobSubType() == ValidationBatchJobSubType.INTERNAL) {
      generatePatternAnalysis(originSuccessExecutionRecordDTO);
    }
    return resultExecutionRecordDTO;
  }

  @PostConstruct
  private void postConstruct() {
    switch ((ValidationBatchJobSubType) getFullBatchJobType().getBatchJobSubType()) {
      case EXTERNAL -> schema = "EDM-EXTERNAL";
      case INTERNAL -> schema = "EDM-INTERNAL";
    }
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, SuccessExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.getSuccessExecutionRecordDTO();
      final String reorderedFileContent;
      reorderedFileContent = reorderFileContent(originSuccessExecutionRecordDTO.getRecordData());

      ValidationResult result = validationExecutionService.singleValidation(schema, null, null, reorderedFileContent);
      if (result.isSuccess()) {
        LOGGER.debug("Validation Success for datasetId {}, recordId {}", originSuccessExecutionRecordDTO.getDatasetId(),
            originSuccessExecutionRecordDTO.getRecordId());
      } else {
        LOGGER.info("Validation Failure for datasetId {}, recordId {}", originSuccessExecutionRecordDTO.getDatasetId(),
            originSuccessExecutionRecordDTO.getRecordId());
        throw new ValidationFailureException(result.getMessage());
      }

      return createCopyIdentifiersValidated(
          originSuccessExecutionRecordDTO,
          jobMetadataDTO.getTargetExecutionId(),
          jobMetadataDTO.getTargetExecutionName(),
          b -> b.recordData(originSuccessExecutionRecordDTO.getRecordData()));
    };
  }

  private void generatePatternAnalysis(SuccessExecutionRecordDTO successExecutionRecordDTO) {
    Optional<ExecutionPoint> executionPoint = executionPointRepository.findFirstByDatasetIdAndExecutionNameOrderByExecutionTimestampDesc(
        successExecutionRecordDTO.getDatasetId(), getExecutionName());
    if (executionPoint.isEmpty()) {
      throw new IllegalStateException("No execution point found for datasetId " + successExecutionRecordDTO.getDatasetId());
    }
    try {
      patternAnalysisService.generateRecordPatternAnalysis(executionPoint.get(), successExecutionRecordDTO.getRecordData());
    } catch (PatternAnalysisException e) {
      LOGGER.error(format("An error occurred while processing pattern analysis with record id %s",
          successExecutionRecordDTO.getRecordId()), e);
    }
  }

  private String reorderFileContent(String recordData) throws TransformationException {
    LOGGER.debug("Reordering the file");
    try (XsltTransformer xsltTransformer = xsltTransformerFactory.getObject()) {
      StringWriter writer = xsltTransformer.transform(recordData.getBytes(StandardCharsets.UTF_8), null);
      return writer.toString();
    }
  }

  @StandardException
  private static class ValidationFailureException extends Exception {

  }
}
