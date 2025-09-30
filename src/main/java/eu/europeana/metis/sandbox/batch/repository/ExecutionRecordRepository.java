package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.reader.DefaultRepositoryItemReader;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link ExecutionRecord} entities.
 */
@Repository
public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, Long> {

  /**
   * Retrieves a paginated list of ExecutionRecord entities based on dataset ID and execution ID excluding duplicates.
   *
   * <p>This is used by the {@link DefaultRepositoryItemReader} therefore it is marked as unused from the IDE.
   *
   * @param datasetId The ID of the dataset.
   * @param executionId The ID of the execution.
   * @param pageable The paging configuration, including page number and size.
   * @return A paginated list of ExecutionRecord entities excluding duplicates matching the criteria.
   */
  @Query("""
      SELECT r
      FROM ExecutionRecord r
      WHERE r.id IN (
          SELECT MIN(r2.id)
          FROM ExecutionRecord r2
          WHERE r2.execution.datasetId = :datasetId
            AND r2.execution.executionId = :executionId
          GROUP BY r2.identifier.recordId
      )
      """)
  Page<ExecutionRecord> findCanonicalRecords(
      @Param("datasetId") String datasetId,
      @Param("executionId") String executionId,
      Pageable pageable
  );

  /**
   * Finds duplicate execution records for a given dataset and execution name.
   * <p>
   * A record is considered duplicate if it shares the same recordId within the provided datasetId and executionName, and is not
   * the record with the smallest ID in the same group.
   *
   * @param datasetId the unique identifier for the dataset used to filter records.
   * @param executionName the name of the execution used to filter records.
   * @return a list of duplicate {@code ExecutionRecord} entities that match the specified datasetId and executionName criteria.
   */
  @Query("""
      SELECT r
      FROM ExecutionRecord r
      WHERE r.execution.datasetId = :datasetId
        AND r.execution.executionName = :executionName
        AND r.id NOT IN (
            SELECT MIN(r2.id)
            FROM ExecutionRecord r2
            WHERE r2.execution.datasetId = :datasetId
              AND r2.execution.executionName = :executionName
            GROUP BY r2.identifier.recordId
        )
      """)
  List<ExecutionRecord> findDuplicateRecords(
      @Param("datasetId") String datasetId,
      @Param("executionName") String executionName
  );

  /**
   * Counts the duplicate records in the database for a specific dataset and execution name.
   * <p>
   * A record is considered a duplicate if it shares the same dataset ID and execution name and is not the record with the minimum
   * ID for the same record ID.
   *
   * @param datasetId the identifier of the dataset to filter the records
   * @param executionName the name of the execution to filter the records
   * @return the count of duplicate records matching the given dataset ID and execution name
   */
  @Query("""
      SELECT COUNT(r)
      FROM ExecutionRecord r
      WHERE r.execution.datasetId = :datasetId
        AND r.execution.executionName = :executionName
        AND r.id NOT IN (
            SELECT MIN(r2.id)
            FROM ExecutionRecord r2
            WHERE r2.execution.datasetId = :datasetId
              AND r2.execution.executionName = :executionName
            GROUP BY r2.identifier.recordId
        )
      """)
  long countDuplicateRecords(@Param("datasetId") String datasetId, @Param("executionName") String executionName);

  /**
   * Retrieves an ExecutionRecord based on the provided dataset ID, record ID, and execution name.
   *
   * @param datasetId The ID of the dataset.
   * @param recordId The ID of the record within the dataset.
   * @param executionName The name of the execution.
   * @return The matching ExecutionRecord or null if no match is found.
   */
  ExecutionRecord findByExecution_DatasetIdAndIdentifier_RecordIdAndExecution_ExecutionName(String datasetId, String recordId,
      String executionName);

  /**
   * Counts the number of ExecutionRecord entities matching the given dataset ID and execution name.
   *
   * @param datasetId The ID of the dataset.
   * @param executionName The set of execution names to filter the records by.
   * @return The count of ExecutionRecord entities matching the specified criteria.
   */
  long countByExecution_DatasetIdAndExecution_ExecutionName(String datasetId, String executionName);

  /**
   * Retrieves dataset statistics by grouping ExecutionRecord entities based on dataset IDs.
   *
   * @return A list of projections containing dataset IDs and their respective counts.
   */
  @Query("""
      SELECT r.execution.datasetId AS datasetId, COUNT(r) AS count 
            FROM ExecutionRecord r GROUP BY r.execution.datasetId
      """)
  List<DatasetStatisticProjection> getDatasetStatistics();

  /**
   * Retrieves step statistics by grouping ExecutionRecord entities based on execution names.
   *
   * @return A list of projections containing step names and their respective counts.
   */
  @Query("""
      SELECT r.execution.executionName AS step, COUNT(r) AS count 
            FROM ExecutionRecord r 
            GROUP BY r.execution.executionName
      """)
  List<StepStatisticProjection> getStepStatistics();

  /**
   * Deletes all ExecutionRecord entities associated with the specified dataset ID.
   *
   * @param datasetId The ID of the dataset for which records will be deleted.
   */
  void removeByExecution_DatasetId(String datasetId);

  /**
   * Projection interface representing dataset statistics.
   *
   * <p>Provides access to dataset ID and the associated count of execution records.
   * <p>Used in queries to get aggregated statistics for datasets.
   */
  interface DatasetStatisticProjection {

    String getDatasetId();

    long getCount();
  }

  /**
   * Projection interface representing step statistics.
   *
   * <p>Provides access to step names and the associated count of execution records.
   * <p>Used in queries to get aggregated statistics for steps.
   */
  interface StepStatisticProjection {

    String getStep();

    long getCount();
  }
}
