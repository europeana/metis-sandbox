package eu.europeana.metis.sandbox.repository.debias;


import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasMainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

/**
 * The interface Record DeBias main repository.
 */
public interface RecordDeBiasMainRepository extends JpaRepository<RecordDeBiasMainEntity, Long> {

  /**
   * Delete by record id dataset id.
   *
   * @param datasetId the dataset id
   */
  @Modifying
  void deleteByRecordIdDatasetId(String datasetId);
}
