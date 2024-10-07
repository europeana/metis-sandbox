package eu.europeana.metis.sandbox.repository.debias;

import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;


/**
 * The interface Dataset DeBias repository.
 */
public interface DatasetDeBiasRepository extends JpaRepository<DatasetDeBiasEntity, Long> {

  /**
   * Find detection entity by dataset id detection entity.
   *
   * @param datasetId the dataset id
   * @return the detection entity
   */
  DatasetDeBiasEntity findDetectionEntityByDatasetId_DatasetId(Integer datasetId);

  /**
   * Update state.
   *
   * @param datasetId the dataset id
   * @param state the state
   */
  @Modifying
  @Query("UPDATE DatasetDeBiasEntity dec SET dec.state = ?2 WHERE dec.datasetId.datasetId = ?1")
  void updateState(Integer datasetId, String state);

}
