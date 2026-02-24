package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;

import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.ExceptionInfoDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.service.workflow.EnrichService;
import eu.europeana.metis.sandbox.service.workflow.EnrichService.EnrichmentProcessingResult;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

/**
 * Processor responsible for enrichment.
 */
@StepScope
@Component("enrichItemProcessor")
public class EnrichItemProcessor extends AbstractExecutionRecordMetisItemProcessor {

  private final EnrichService enrichService;

  /**
   * Constructor with service parameter.
   *
   * @param enrichService The service responsible for enriching record data.
   */
  public EnrichItemProcessor(EnrichService enrichService) {
    this.enrichService = enrichService;
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, AbstractExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.getSuccessExecutionRecordDTO();
      EnrichmentProcessingResult enrichmentProcessingResult = enrichService.enrichRecord(
          originSuccessExecutionRecordDTO.getRecordData());

      Set<ExceptionInfoDTO> exceptionInfoDTOs = enrichmentProcessingResult.warningExceptions().stream()
                                                                          .map(ExceptionInfoDTO::from)
                                                                          .collect(Collectors.toSet());
      return createCopyIdentifiersValidated(
          originSuccessExecutionRecordDTO,
          jobMetadataDTO.getTargetExecutionId(),
          jobMetadataDTO.getTargetExecutionName(),
          builder -> builder
              .recordData(enrichmentProcessingResult.processedRecord())
              .exceptionWarnings(exceptionInfoDTOs));
    };
  }

}
