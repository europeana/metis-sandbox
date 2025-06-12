package eu.europeana.metis.sandbox.repository.debias;

import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasDetailEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The interface Record DeBias detail repository.
 */
public interface RecordDeBiasDetailRepository extends JpaRepository<RecordDeBiasDetailEntity, Long> {


  /**
   * Find by debias id list.
   *
   * @param debiasId the debias id
   * @return the list
   */
  List<RecordDeBiasDetailEntity> findByDebiasIdId(Long debiasId);

  @Modifying
  @Query("""
      DELETE FROM RecordDeBiasDetailEntity rdd
            WHERE rdd.debiasId.id IN (SELECT rdm.id FROM RecordDeBiasMainEntity rdm WHERE rdm.datasetId.datasetId = :datasetId)
      """)
  void deleteByDatasetId(@Param("datasetId") String datasetId);
}
