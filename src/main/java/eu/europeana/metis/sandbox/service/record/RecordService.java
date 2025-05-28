package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.dto.RecordTiersInfoDto;
import java.util.List;

public interface RecordService {

  /**
   * Returns a list of records and their data associated to a given dataset id
   *
   * @param datasetId the id of the dataset that the records belong to
   * @return A list of RecordTiersInfoDto associated to the given dataset id
   */
  List<RecordTiersInfoDto> getRecordsTiers(String datasetId);
  /**
   * Deletes all records that are associated to a given dataset id
   * @param datasetId The id of the dataset
   */
  void remove(String datasetId);

}
