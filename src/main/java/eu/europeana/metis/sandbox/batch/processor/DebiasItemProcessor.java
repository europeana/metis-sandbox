package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;

import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.service.debias.DeBiasProcessService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

/**
 * Processor responsible for debias.
 */
@StepScope
@Component("debiasItemProcessor")
public class DebiasItemProcessor extends AbstractExecutionRecordMetisItemProcessor {

  private final DeBiasProcessService deBiasProcessService;

  /**
   * Constructor with service parameter.
   *
   * @param deBiasProcessService The service responsible for debiasing record data.
   */
  public DebiasItemProcessor(DeBiasProcessService deBiasProcessService) {
    this.deBiasProcessService = deBiasProcessService;
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, AbstractExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.successExecutionRecordDTO();

      deBiasProcessService.process(
          originSuccessExecutionRecordDTO.getRecordData(),
          originSuccessExecutionRecordDTO.getDatasetId(),
          originSuccessExecutionRecordDTO.getRecordId());

      return createCopyIdentifiersValidated(
          originSuccessExecutionRecordDTO,
          jobMetadataDTO.targetExecutionId(),
          jobMetadataDTO.targetExecutionName(),
          b -> b.recordData(originSuccessExecutionRecordDTO.getRecordData()));
    };
  }

}

