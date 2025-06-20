package eu.europeana.metis.sandbox.batch.processor;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordConverter;
import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.FailExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for processing ExecutionRecord objects into ExecutionRecordDTO objects.
 *
 * <p>Provides a framework for handling item processing with error handling.
 * <p>Extends the functionality of AbstractMetisItemProcessor with specific handling for execution records.
 * <p>This class defines a standardized process flow, converting entities, adding metadata, and handling exceptions.
 */
public abstract class AbstractExecutionRecordMetisItemProcessor extends
    AbstractMetisItemProcessor<ExecutionRecord, AbstractExecutionRecordDTO> {

  @Override
  public AbstractExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    SuccessExecutionRecordDTO originSuccessExecutionRecordDTO =
        ExecutionRecordConverter.convertToExecutionRecordDTO(executionRecord);

    JobMetadataDTO jobMetadataDTO =
        new JobMetadataDTO(originSuccessExecutionRecordDTO, getExecutionName(), getTargetExecutionId());

    return processCapturingException(jobMetadataDTO, getProcessRecordFunction(), defaultHandler()
    );
  }

  /**
   * Provides a default error handling function for transforming failed job metadata into a failure execution record DTO.
   *
   * <p>The returned function takes a JobMetadataDTO and an Exception and produces a FailExecutionRecordDTO with details from
   * the provided metadata and exception.
   *
   * @return A BiFunction that converts JobMetadataDTO and Exception into a FailExecutionRecordDTO.
   */
  public static BiFunction<JobMetadataDTO, Exception, AbstractExecutionRecordDTO> defaultHandler() {
    return (jobMetadataDTO, exception) -> FailExecutionRecordDTO.createValidated(
        b -> b
            .datasetId(jobMetadataDTO.getSuccessExecutionRecordDTO().getDatasetId())
            .recordId(jobMetadataDTO.getSuccessExecutionRecordDTO().getRecordId())
            .executionId(jobMetadataDTO.getTargetExecutionId())
            .executionName(jobMetadataDTO.getTargetExecutionName())
            .exception(exception)
    );
  }

}
