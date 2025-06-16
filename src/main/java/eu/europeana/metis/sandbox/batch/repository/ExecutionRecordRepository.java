package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import eu.europeana.metis.sandbox.batch.reader.DefaultRepositoryItemReader;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing ExecutionRecordRepository entities.
 */
@Repository
public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, ExecutionRecordIdentifierKey> {

  /**
   * Retrieves a paginated list of ExecutionRecord entities based on dataset ID and execution ID.
   *
   * <p>This is used by the {@link DefaultRepositoryItemReader} therefore it is marked as unused from the IDE.
   *
   * @param datasetId The ID of the dataset.
   * @param executionId The ID of the execution.
   * @param pageable The paging configuration, including page number and size.
   * @return A paginated list of ExecutionRecord entities matching the criteria.
   */
  Page<ExecutionRecord> findByIdentifier_DatasetIdAndIdentifier_ExecutionId(String datasetId, String executionId,
      Pageable pageable);

  /**
   * Retrieves an ExecutionRecord based on the provided dataset ID, record ID, and execution name.
   *
   * @param datasetId The ID of the dataset.
   * @param recordId The ID of the record within the dataset.
   * @param executionName The name of the execution.
   * @return The matching ExecutionRecord or null if no match is found.
   */
  ExecutionRecord findByIdentifier_DatasetIdAndIdentifier_RecordIdAndIdentifier_ExecutionName(String datasetId, String recordId,
      String executionName);

  /**
   * Finds a set of ExecutionRecord objects based on dataset ID, record ID, and a set of execution names.
   *
   * @param datasetId The ID of the dataset. * @param recordId The ID of the record within the dataset.
   * @param executionName The set of execution names to filter the records by.
   * @return A set of ExecutionRecord objects matching the given identifiers and execution names.
   */
  Set<ExecutionRecord> findByIdentifier_DatasetIdAndIdentifier_RecordIdAndIdentifier_ExecutionNameIn(String datasetId,
      String recordId, Set<String> executionName);

  /**
   * Counts the number of ExecutionRecord entities matching the given dataset ID and execution name.
   *
   * @param datasetId The ID of the dataset. * @param recordId The ID of the record within the dataset.
   * @return The count of ExecutionRecord entities matching the specified criteria.
   */
  long countByIdentifier_DatasetIdAndIdentifier_ExecutionName(String datasetId, String executionName);

  /**
   * Retrieves dataset statistics by grouping ExecutionRecord entities based on dataset IDs.
   *
   * @return A list of projections containing dataset IDs and their respective counts.
   */
  @Query("""
      SELECT r.identifier.datasetId AS datasetId, COUNT(r) AS count 
            FROM ExecutionRecord r GROUP BY r.identifier.datasetId
      """)
  List<DatasetStatisticProjection> getDatasetStatistics();

  /**
   * Retrieves step statistics by grouping ExecutionRecord entities based on execution names.
   *
   * @return A list of projections containing step names and their respective counts.
   */
  @Query("""
      SELECT r.identifier.executionName AS step, COUNT(r) AS count 
            FROM ExecutionRecord r 
            GROUP BY r.identifier.executionName
      """)
  List<StepStatisticProjection> getStepStatistics();

  /**
   * Deletes all ExecutionRecord entities associated with the specified dataset ID.
   *
   * @param datasetId The ID of the dataset for which records will be deleted.
   */
  void removeByIdentifier_DatasetId(String datasetId);

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
