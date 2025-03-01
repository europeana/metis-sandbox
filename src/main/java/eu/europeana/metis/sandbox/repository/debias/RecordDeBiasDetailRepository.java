package eu.europeana.metis.sandbox.repository.debias;

import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasDetailEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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

  /**
   * Delete by debias id record id dataset id.
   *
   * @param datasetId the dataset id
   */
  @Modifying
  @Query("DELETE FROM RecordDeBiasDetailEntity rdd WHERE EXISTS "
      + "(SELECT 1 FROM RecordDeBiasMainEntity rdm INNER JOIN RecordEntity rec ON rec.id=rdm.recordId.id AND rec.datasetId = ?1 "
      + "WHERE rdd.debiasId.id = rdm.id)")
  void deleteByDebiasIdRecordIdDatasetId(String datasetId);
}
