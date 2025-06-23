package eu.europeana.metis.sandbox.repository.debias;


import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasMainEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for managing RecordDeBiasMainEntity entities.
 */
public interface RecordDeBiasMainRepository extends JpaRepository<RecordDeBiasMainEntity, Long> {

  /**
   * Retrieves a list of RecordDeBiasMainEntity instances associated with the given dataset ID.
   *
   * @param datasetId the ID of the dataset to filter by
   * @return a list of matching RecordDeBiasMainEntity objects
   */
  List<RecordDeBiasMainEntity> findByDatasetId_DatasetId(Integer datasetId);

  /**
   * Deletes all RecordDeBiasMainEntity entries associated with a specific dataset ID.
   *
   * @param datasetId the dataset identifier
   */
  @Modifying
  @Query("DELETE FROM RecordDeBiasMainEntity rdm WHERE rdm.datasetId.datasetId = :datasetId")
  void deleteByDatasetId(@Param("datasetId") String datasetId);
}
