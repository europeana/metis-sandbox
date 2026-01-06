package eu.europeana.metis.sandbox.repository.debias;

import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


/**
 * Repository interface for managing DatasetDeBiasEntity entities.
 */
public interface DatasetDeBiasRepository extends JpaRepository<DatasetDeBiasEntity, Long> {

  /**
   * Find detection entity by dataset id detection entity.
   *
   * @param datasetId the dataset id
   * @return the dataset debias entity
   */
  DatasetDeBiasEntity findDetectionEntityByDatasetIdDatasetId(Integer datasetId);

  /**
   * Deletes all DatasetDeBiasEntity entries associated with the specified dataset ID.
   *
   * @param datasetId the id of the dataset whose associated records are to be deleted
   */
  @Modifying
  @Query("DELETE FROM DatasetDeBiasEntity dec WHERE dec.datasetId.datasetId = :datasetId")
  void deleteByDatasetId(@Param("datasetId") String datasetId);
}
