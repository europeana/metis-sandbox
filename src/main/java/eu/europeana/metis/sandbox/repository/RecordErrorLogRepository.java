package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.RecordErrorLogEntity;
import eu.europeana.metis.sandbox.entity.projection.ErrorLogView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * A {@link JpaRepository} that stores records error logs.
 * <p>A record error log is a representation of an error for a record per each step.</p>
 */
public interface RecordErrorLogRepository extends JpaRepository<RecordErrorLogEntity, Long> {

  /**
   * Get all errors by dataset id
   *
   * @param datasetId must not be null
   * @return list of errors
   * @see ErrorLogView
   * @see <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation">Query
   * Creation</a>
   * @see <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#projections">Projections</a>
   */
  List<ErrorLogView> getByRecordIdDatasetId(String datasetId);

  /**
   * Get record given a record id, dataset id and step.
   * <p>The record id will be searched against both {@link RecordErrorLogEntity#getRecordId().getProviderId()} and {@link
   * RecordErrorLogEntity#getRecordId().getEuropeanaId()}.</p>
   *
   * @param recordId the record id
   * @param datasetId the dataset id
   * @param step the step
   * @return the record error log
   */
  @Query("SELECT rele FROM RecordErrorLogEntity rele WHERE (rele.recordId.providerId = ?1 OR rele.recordId.europeanaId= ?1) " +
          "AND rele.recordId.datasetId = ?2 AND rele.step = ?3 ")
  RecordErrorLogEntity findRecordLogByRecordIdAndDatasetIdAndStep(String recordId, String datasetId, Step step);

  /**
   * Delete errors for the specified dataset id
   *
   * @param datasetId must not be null
   */
  @Modifying
  @Query("DELETE FROM RecordErrorLogEntity ele WHERE EXISTS (SELECT 1 FROM RecordEntity rec "
      + "WHERE rec.id = ele.recordId.id AND rec.datasetId = ?1)")
  void deleteByRecordIdDatasetId(String datasetId);
}
