package eu.europeana.metis.sandbox.repository.debias;

import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


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
  DatasetDeBiasEntity findDetectionEntityByDatasetIdDatasetId(Integer datasetId);

  /**
   * Update state.
   *
   * @param datasetId the dataset id
   * @param state the state
   */
  @Modifying
  @Query("UPDATE DatasetDeBiasEntity dec SET dec.state = :state WHERE dec.datasetId.datasetId = :datasetId")
  void updateState(@Param("datasetId") Integer datasetId, @Param("state") String state);

  /**
   * Delete by dataset id.
   *
   * @param datasetId the dataset id
   */
  @Modifying
  @Query("DELETE FROM DatasetDeBiasEntity dec WHERE dec.datasetId.datasetId = :datasetId")
  void deleteByDatasetId(@Param("datasetId") String datasetId);
}
