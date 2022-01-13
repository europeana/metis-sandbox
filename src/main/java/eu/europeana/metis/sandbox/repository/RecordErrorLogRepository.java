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
   * @see <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation">Query Creation</a>
   * @see <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#projections">Projections</a>
   */
  List<ErrorLogView> getByRecordId_DatasetId(String datasetId);

  /**
   * Delete errors for the specified dataset id
   *
   * @param datasetId must not be null
   */
  @Modifying
  @Query("delete from RecordErrorLogEntity where recordId.datasetId = ?1")
  void deleteByDatasetId(String datasetId);
}
