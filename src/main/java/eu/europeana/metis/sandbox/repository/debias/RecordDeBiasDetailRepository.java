package eu.europeana.metis.sandbox.repository.debias;

import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

/**
 * The interface Record DeBias detail repository.
 */
public interface RecordDeBiasDetailRepository extends JpaRepository<RecordDeBiasDetailEntity, Long> {

  /**
   * Delete by debias id record id dataset id.
   *
   * @param datasetId the dataset id
   */
  @Modifying
  void deleteByDebiasIdRecordIdDatasetId(String datasetId);
}
