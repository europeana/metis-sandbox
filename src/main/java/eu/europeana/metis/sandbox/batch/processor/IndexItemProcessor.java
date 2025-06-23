package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;

import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.service.workflow.IndexService;
import eu.europeana.metis.sandbox.service.workflow.IndexService.IndexingResult;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

/**
 * Processor responsible for indexing.
 */
@StepScope
@Component("indexItemProcessor")
public class IndexItemProcessor extends AbstractExecutionRecordMetisItemProcessor {

  private final IndexService indexService;

  /**
   * Constructor with service parameter.
   *
   * @param indexService The service responsible for indexing record data.
   */
  public IndexItemProcessor(IndexService indexService) {
    this.indexService = indexService;
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, AbstractExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.getSuccessExecutionRecordDTO();
      IndexingResult result = indexService.indexRecord(
          originSuccessExecutionRecordDTO.getRecordId(),
          originSuccessExecutionRecordDTO.getRecordData()
      );
      return createCopyIdentifiersValidated(
          originSuccessExecutionRecordDTO,
          jobMetadataDTO.getTargetExecutionId(),
          jobMetadataDTO.getTargetExecutionName(),
          b -> b.recordData(result.recordData())
                .tierResults(result.tierResults()));
    };
  }
}
