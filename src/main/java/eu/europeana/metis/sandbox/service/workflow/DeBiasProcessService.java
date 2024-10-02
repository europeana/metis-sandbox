package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import java.util.List;

/**
 * The interface De Bias process service.
 */
public interface DeBiasProcessService {

  /**
   * Process.
   *
   * @param recordsToProcess the records to process
   */
  void process(List<Record> recordsToProcess);
}
