package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.RecordErrorLogEntity;
import eu.europeana.metis.sandbox.entity.projection.ErrorLogView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RecordErrorLogRepository extends JpaRepository<RecordErrorLogEntity, Long> {

  /**
   * Get all errors by dataset id
   *
   * @param datasetId must not be null
   * @return list of errors
   * @see ErrorLogView
   */
  List<ErrorLogView> getByDatasetId(String datasetId);

  /**
   * Delete errors for the specified dataset id
   *
   * @param datasetId must not be null
   */
  @Modifying
  @Query("delete from RecordErrorLogEntity where datasetId = ?1")
  void deleteByDatasetId(String datasetId);
}
