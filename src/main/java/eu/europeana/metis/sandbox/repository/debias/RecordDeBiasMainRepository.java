package eu.europeana.metis.sandbox.repository.debias;


import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasMainEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * The interface Record DeBias main repository.
 */
public interface RecordDeBiasMainRepository extends JpaRepository<RecordDeBiasMainEntity, Long> {


  List<RecordDeBiasMainEntity> findByDatasetId_DatasetId(Integer datasetId);

  @Modifying
  @Query("DELETE FROM RecordDeBiasMainEntity rdm WHERE rdm.datasetId.datasetId = ?1")
  void deleteByDatasetId(String datasetId);
}
