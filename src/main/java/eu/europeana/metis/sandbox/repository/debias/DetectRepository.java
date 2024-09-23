package eu.europeana.metis.sandbox.repository.debias;

import eu.europeana.metis.sandbox.entity.debias.DetectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * The interface Detect repository.
 */
public interface DetectRepository extends JpaRepository<DetectionEntity, Long> {

  /**
   * Find detection entity by dataset id detection entity.
   *
   * @param datasetId the dataset id
   * @return the detection entity
   */
  DetectionEntity findDetectionEntityByDatasetId_DatasetId(Integer datasetId);

  /**
   * Update state.
   *
   * @param datasetId the dataset id
   * @param state the state
   */
  @Modifying
  @Query("UPDATE DetectionEntity dec SET dec.state = ?2 WHERE dec.datasetId.datasetId = ?1")
  void updateState(Integer datasetId, String state);

}
