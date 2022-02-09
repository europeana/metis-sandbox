package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.domain.Record;

public interface RecordService {

  /**
   * Updates record's europeana id and provider id values
   *
   * @param recordToUpdate the record to update
   */
  void setEuropeanaIdAndProviderId(Record recordToUpdate);

}
