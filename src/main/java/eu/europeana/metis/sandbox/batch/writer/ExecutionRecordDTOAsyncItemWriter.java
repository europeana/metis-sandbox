package eu.europeana.metis.sandbox.batch.writer;

import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.stereotype.Component;

/**
 * Asynchronous ItemWriter implementation for handling {@link ExecutionRecordDTO} objects.
 *
 * <p>Delegates the writing of {@link ExecutionRecordDTO} objects to an instance of
 * {@link ExecutionRecordDTOItemWriter}.
 */
@Component
public class ExecutionRecordDTOAsyncItemWriter extends AsyncItemWriter<ExecutionRecordDTO> {

  /**
   * Constructs an instance of ExecutionRecordDTOAsyncItemWriter by delegating to the provided
   * {@link ExecutionRecordDTOItemWriter}.
   *
   * @param executionRecordDTOItemWriter An instance of {@link ExecutionRecordDTOItemWriter} used to perform the actual writing of
   * {@link ExecutionRecordDTO} objects.
   */
  public ExecutionRecordDTOAsyncItemWriter(ExecutionRecordDTOItemWriter executionRecordDTOItemWriter) {
    super();
    setDelegate(executionRecordDTOItemWriter);
  }
}
