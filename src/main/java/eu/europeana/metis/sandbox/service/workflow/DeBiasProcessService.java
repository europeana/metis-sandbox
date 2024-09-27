package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import java.util.List;

/**
 * The interface De bias process service.
 */
public interface DeBiasProcessService {

  /**
   * Process record info.
   *
   * @param recordToProcess the record to process
   * @return the record info
   */
  List<RecordInfo> process(List<Record> recordToProcess);
}
