package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordWarning;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository.StepStatisticProjection;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link ExecutionRecordWarning} entities.
 */
@Repository
public interface ExecutionRecordWarningRepository extends JpaRepository<ExecutionRecordWarning, Long> {

  /**
   * Finds a list of ExecutionRecordWarningException entities based on the dataset ID and executionRun name.
   *
   * @param datasetId The ID of the dataset.
   * @param executionName The name of the executionRun associated with the executionRun record.
   * @return A list of ExecutionRecordWarningException entities matching the given criteria.
   */
  @Query("""
      SELECT r.executionRecord.identifier AS identifier,
             r.message AS message
        FROM ExecutionRecordWarning r
        WHERE r.executionRecord.executionRun.datasetId = :datasetId
          AND r.executionRecord.executionRun.executionName = :executionName
      """)
  Stream<ExecutionRecordWarningProjection> findWarningsWithIdentifiers(
      @Param("datasetId") String datasetId,
      @Param("executionName") String executionName);

  /**
   * Counts the number of distinct entities by recordId and based on the dataset ID and executionRun name.
   *
   * @param datasetId The ID of the dataset.
   * @param executionName The name of the executionRun associated with the entities.
   * @return The count of entities matching the specified dataset ID and executionRun name.
   */
  @Query("""
          SELECT COUNT(DISTINCT w.executionRecord.identifier.recordId)
          FROM ExecutionRecordWarning w
          WHERE w.executionRecord.executionRun.datasetId = :datasetId
            AND w.executionRecord.executionRun.executionName = :executionName
      """)
  long countDistinctRecordIds(@Param("datasetId") String datasetId, @Param("executionName") String executionName);

  /**
   * Retrieves statistics of executionRun steps, including the step name and the count of records grouped per step.
   *
   * @return A list of StepStatisticProjection containing the step name and the corresponding count.
   */
  @Query("""
      SELECT er.executionRun.executionName AS step, COUNT(er) AS count 
            FROM ExecutionRecord er 
            GROUP BY er.executionRun.executionName
      """)
  List<StepStatisticProjection> getStepStatistics();

  /**
   * Removes entities associated with the specified dataset ID.
   *
   * @param datasetId The ID of the dataset.
   */
  void removeByExecutionRecord_ExecutionRun_DatasetId(String datasetId);

  /**
   * Projection interface for exposing specific fields related to execution record warnings.
   */
  interface ExecutionRecordWarningProjection {

    ExecutionRecordIdentifier getIdentifier();

    String getMessage();
  }

}
