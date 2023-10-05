package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.DatasetLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetLogRepository extends JpaRepository<DatasetLogEntity, Long> {

  /**
   * Return a list of logs associated to the given dataset
   * @param datasetId The id of the dataset we want to get the logs from
   * @return A list of logs of the given dataset
   */
  List<DatasetLogEntity> findByDatasetDatasetId(int datasetId);

  /**
   * Removes all logs associated with the given dataset id
   * @param datasetId The id of the dataset to removes its logs
   */
  void deleteAllByDatasetDatasetId(int datasetId);
}
