package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordAndDTOConverterUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.common.TransformationBatchJobSubType;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.sandbox.service.workflow.TransformService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@StepScope
@Component("transformationItemProcessor")
public class TransformerItemProcessor extends AbstractMetisItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

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
  private final TransformXsltRepository transformXsltRepository;
  private final ItemProcessorUtil itemProcessorUtil;

  public TransformerItemProcessor(TransformService transformService, TransformXsltRepository transformXsltRepository) {
    this.transformService = transformService;
    this.transformXsltRepository = transformXsltRepository;
    itemProcessorUtil = new ItemProcessorUtil(getProcessRecordFunction());
  }

  @Override
  public ExecutionRecordDTO process(@NonNull ExecutionRecord executionRecord) {
    final SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = ExecutionRecordAndDTOConverterUtil.converterToExecutionRecordDTO(
        executionRecord);
    JobMetadataDTO jobMetadataDTO = new JobMetadataDTO(originSuccessExecutionRecordDTO, getExecutionName(),
        getTargetExecutionId());
    return itemProcessorUtil.processCapturingException(jobMetadataDTO);
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, SuccessExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.getSuccessExecutionRecordDTO();
      String xsltContent = transformXsltRepository.findById(Integer.valueOf(xsltId))
                                                  .map(TransformXsltEntity::getTransformXslt).orElseThrow();

      final String resultString = transformService.transformRecord(
          originSuccessExecutionRecordDTO.getRecordId(),
          originSuccessExecutionRecordDTO.getRecordData(),
          xsltId,
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
