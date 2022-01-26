package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService.RecordIdType;

public interface RecordService {

  /**
   * Updates record's europeana id and provider id values
   *
   * @param record the record to update
   */
  void setEuropeanaIdAndProviderId(Record record);

}
