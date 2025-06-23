package eu.europeana.metis.sandbox.repository.debias;

import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasDetailEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for managing RecordDeBiasDetailEntity entities.
 */
public interface RecordDeBiasDetailRepository extends JpaRepository<RecordDeBiasDetailEntity, Long> {

  /**
   * Retrieves a list of RecordDeBiasDetailEntity instances associated with the given debias ID.
   *
   * @param debiasId the ID of the debias entity to filter by
   * @return a list of associated RecordDeBiasDetailEntity objects
   */
  List<RecordDeBiasDetailEntity> findByDebiasIdId(Long debiasId);

  /**
   * Deletes all RecordDeBiasDetailEntity entries associated with a specific dataset ID.
   *
   * @param datasetId the dataset ID used to identify related records for deletion
   */
  @Modifying
  @Query("""
      DELETE FROM RecordDeBiasDetailEntity rdd
            WHERE rdd.debiasId.id IN (SELECT rdm.id FROM RecordDeBiasMainEntity rdm WHERE rdm.datasetId.datasetId = :datasetId)
      """)
  void deleteByDatasetId(@Param("datasetId") String datasetId);
}
