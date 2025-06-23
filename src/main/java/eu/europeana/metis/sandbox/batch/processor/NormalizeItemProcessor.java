package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;

import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.service.workflow.NormalizeService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

/**
 * Processor responsible for normalization.
 */
@StepScope
@Component("normalizeItemProcessor")
public class NormalizeItemProcessor extends AbstractExecutionRecordMetisItemProcessor {

  private final NormalizeService normalizeService;

  /**
   * Constructor with service parameter.
   *
   * @param normalizeService The service responsible for normalizing record data.
   */
  public NormalizeItemProcessor(NormalizeService normalizeService) {
    this.normalizeService = normalizeService;
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, AbstractExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.getSuccessExecutionRecordDTO();
      String result = normalizeService.normalizeRecord(originSuccessExecutionRecordDTO.getRecordData());

      return createCopyIdentifiersValidated(
          originSuccessExecutionRecordDTO,
          jobMetadataDTO.getTargetExecutionId(),
          jobMetadataDTO.getTargetExecutionName(),
          b -> b.recordData(result));
    };
  }
}
