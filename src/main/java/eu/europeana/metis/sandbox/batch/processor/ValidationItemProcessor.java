package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;

import eu.europeana.metis.sandbox.batch.common.ValidationBatchJobSubType;
import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.service.workflow.ValidationService;
import eu.europeana.metis.sandbox.service.workflow.ValidationService.ValidationResultWithIdentifiers;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

/**
 * Processor responsible for validation.
 */
@StepScope
@Component("validationItemProcessor")
public class ValidationItemProcessor extends AbstractExecutionRecordMetisItemProcessor {

  private final ValidationService validationService;

  /**
   * Constructor with service parameter.
   *
   * @param validationService The service responsible for validating record data.
   */
  public ValidationItemProcessor(ValidationService validationService) {
    this.validationService = validationService;
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, AbstractExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.getSuccessExecutionRecordDTO();
      ValidationResultWithIdentifiers validationResultWithIdentifiers = validationService.validateRecord(
          originSuccessExecutionRecordDTO.getRecordData(),
          originSuccessExecutionRecordDTO.getRecordId(),
          originSuccessExecutionRecordDTO.getDatasetId(),
          getExecutionName(),
          (ValidationBatchJobSubType) getFullBatchJobType().getBatchJobSubType()
      );

      String sourceProvidedChoAbout =
          validationResultWithIdentifiers.europeanaGeneratedIdsMap().map(EuropeanaGeneratedIdsMap::getSourceProvidedChoAbout)
                                         .orElse(originSuccessExecutionRecordDTO.getSourceRecordId());
      String europeanaId = validationResultWithIdentifiers.europeanaGeneratedIdsMap()
                                                          .map(EuropeanaGeneratedIdsMap::getEuropeanaGeneratedId)
                                                          .orElse(originSuccessExecutionRecordDTO.getRecordId());

      return createCopyIdentifiersValidated(
          originSuccessExecutionRecordDTO,
          jobMetadataDTO.getTargetExecutionId(),
          jobMetadataDTO.getTargetExecutionName(),
          builder ->
              builder.recordData(originSuccessExecutionRecordDTO.getRecordData())
                     .sourceRecordId(sourceProvidedChoAbout)
                     .recordId(europeanaId));
    };
  }
}
