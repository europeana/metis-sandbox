package eu.europeana.metis.sandbox.batch.writer;

import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.stereotype.Component;

/**
 * Asynchronous ItemWriter implementation for handling {@link AbstractExecutionRecordDTO} objects.
 *
 * <p>Delegates the writing of {@link AbstractExecutionRecordDTO} objects to an instance of
 * {@link ExecutionRecordDTOItemWriter}.
 */
@Component
public class ExecutionRecordDTOAsyncItemWriter extends AsyncItemWriter<AbstractExecutionRecordDTO> {

  private final ExecutionRecordDTOItemWriter executionRecordDTOItemWriter;

  /**
   * Constructs an instance of ExecutionRecordDTOAsyncItemWriter by delegating to the provided
   * {@link ExecutionRecordDTOItemWriter}.
   *
   * @param executionRecordDTOItemWriter An instance of {@link ExecutionRecordDTOItemWriter} used to perform the actual writing of
   * {@link AbstractExecutionRecordDTO} objects.
   */
  public ExecutionRecordDTOAsyncItemWriter(ExecutionRecordDTOItemWriter executionRecordDTOItemWriter) {
    super(executionRecordDTOItemWriter);
    this.executionRecordDTOItemWriter = executionRecordDTOItemWriter;
  }

  /**
   * Performs pre-step initialization for the {@link ExecutionRecordDTOItemWriter}.
   */
  @BeforeStep
  public void beforeStep() {
    executionRecordDTOItemWriter.beforeStepInitializeExecutionRun();
  }
}
