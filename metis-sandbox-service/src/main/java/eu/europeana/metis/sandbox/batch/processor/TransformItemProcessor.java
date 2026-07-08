package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;

import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.common.batch.TransformationBatchJobSubType;
import eu.europeana.metis.sandbox.service.workflow.TransformService;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

/**
 * Processor responsible for transformation.
 */
@StepScope
@Component("transformItemProcessor")
public class TransformItemProcessor extends AbstractExecutionRecordMetisItemProcessor {

  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  @Value("#{jobParameters['datasetName']}")
  private String datasetName;
  @Value("#{jobParameters['datasetCountry']}")
  private String datasetCountry;
  @Value("#{jobParameters['datasetLanguage']}")
  private String datasetLanguage;
  @Value("#{jobParameters['xsltId']}")
  private String xsltId;

  private final TransformService transformService;
  private String xsltContent;

  /**
   * Constructor with service parameter.
   *
   * @param transformService The service responsible for transforming record data.
   */
  public TransformItemProcessor(TransformService transformService) {
    this.transformService = transformService;
  }

  @BeforeStep
  public void beforeStep() {
    this.xsltContent = transformService.getXsltContent(xsltId);
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, AbstractExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.getSuccessExecutionRecordDTO();

      final String resultString = transformService.transformRecord(
          originSuccessExecutionRecordDTO.getRecordId(),
          originSuccessExecutionRecordDTO.getRecordData(),
          xsltContent,
          (TransformationBatchJobSubType) getFullBatchJobType().getBatchJobSubType(),
          datasetId,
          datasetName,
          datasetCountry,
          datasetLanguage);

      return createCopyIdentifiersValidated(
          originSuccessExecutionRecordDTO,
          jobMetadataDTO.getTargetExecutionId(),
          jobMetadataDTO.getTargetExecutionName(),
          b -> b.recordData(resultString));
    };
  }
}
