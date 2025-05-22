package eu.europeana.metis.sandbox.batch.writer;

import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.stereotype.Component;

@Component
public class ExecutionRecordDTOAsyncItemWriter extends AsyncItemWriter<ExecutionRecordDTO> {

  public ExecutionRecordDTOAsyncItemWriter(ExecutionRecordDTOItemWriter executionRecordDTOItemWriter) {
    super();
    setDelegate(executionRecordDTOItemWriter);
  }
}
