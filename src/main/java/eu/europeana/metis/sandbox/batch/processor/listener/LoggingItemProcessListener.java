package eu.europeana.metis.sandbox.batch.processor.listener;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdAccess;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import eu.europeana.metis.sandbox.batch.entity.HasExecutionRecordIdAccess;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.Future;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

/**
 * Listener for the item processing lifecycle in a Spring Batch job, logging key events.
 *
 * <p>Logs information before processing, after processing with detailed dataset metadata,
 * and on any processing errors.
 *
 * @param <T> The type of the item, must extend HasExecutionRecordIdAccess with ExecutionRecordIdAccess.
 */
@StepScope
@Component
public class LoggingItemProcessListener<T extends HasExecutionRecordIdAccess<? extends ExecutionRecordIdAccess>>
    implements ItemProcessListener<T, Future<ExecutionRecordDTO>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void beforeProcess(@NotNull T item) {
    LOGGER.debug("beforeProcess");
  }

  @Override
  public void afterProcess(@NotNull T item, Future<ExecutionRecordDTO> future) {
    ExecutionRecordIdAccess executionRecordIdAccess = item.getIdentifier();

    final StringBuilder logBuilder = new StringBuilder(format(
        "Processed datasetId: %s, executionId: %s, executionName: %s, sourceRecordId: %s",
        executionRecordIdAccess.getDatasetId(),
        executionRecordIdAccess.getExecutionId(),
        executionRecordIdAccess.getExecutionName(),
        executionRecordIdAccess.getSourceRecordId()
    ));

    if (executionRecordIdAccess instanceof ExecutionRecordIdentifierKey executionRecordIdentifierKey) {
      logBuilder.append(format(", recordId: %s", executionRecordIdentifierKey.getRecordId()));
    }

    LOGGER.debug(logBuilder.toString());
  }

  @Override
  public void onProcessError(@NotNull T executionRecord, @NotNull Exception e) {
    LOGGER.error(" onProcessError");
  }

}
