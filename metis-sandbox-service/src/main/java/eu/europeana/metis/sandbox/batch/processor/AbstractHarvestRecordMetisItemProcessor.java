package eu.europeana.metis.sandbox.batch.processor;

import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.ExceptionInfoDTO;
import eu.europeana.metis.sandbox.batch.dto.FailExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;

/**
 * Base class for harvesting records from external identifiers, capturing record failures as DTOs.
 */
public abstract class AbstractHarvestRecordMetisItemProcessor extends
    AbstractMetisItemProcessor<ExecutionRecordExternalIdentifier, ExecutionRecordExternalIdentifier, AbstractExecutionRecordDTO> {

  @Value("#{jobParameters['datasetId']}")
  protected String datasetId;

  @Override
  public AbstractExecutionRecordDTO process(@NotNull ExecutionRecordExternalIdentifier identifier) {
    return processCapturingException(identifier, getProcessRecordFunction(), defaultHandler());
  }

  private BiFunction<ExecutionRecordExternalIdentifier, Exception, AbstractExecutionRecordDTO> defaultHandler() {
    return (identifier, exception) -> FailExecutionRecordDTO.createValidated(builder -> builder
        .datasetId(datasetId)
        .executionId(getTargetExecutionId())
        .externalRecordId(identifier.getExternalRecordId())
        .sourceRecordId(identifier.getExternalRecordId())
        .recordId(identifier.getExternalRecordId())
        .executionName(getExecutionName())
        .exceptionInfoDTO(ExceptionInfoDTO.from(exception)));
  }
}
