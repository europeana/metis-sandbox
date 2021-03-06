package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;

public interface TransformationService {

  /**
   * Transform the given record using the default xslt
   *
   * @param record must not be null
   * @return {@link RecordInfo} containing record transformed
   * @throws NullPointerException      if record is null
   * @throws RecordProcessingException if records fails at transformation
   */
  RecordInfo transform(Record record);
}
