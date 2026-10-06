package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.reader.ExternalIdentifiersRepositoryItemReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link ExecutionRecordExternalIdentifier} entities.
 */
@Repository
public interface ExecutionRecordExternalIdentifierRepository extends
    JpaRepository<ExecutionRecordExternalIdentifier, Long> {

  /**
   * Retrieves a paginated list of ExecutionRecordExternalIdentifier entities by their execution ID.
   *
   * <p>This is used by the {@link ExternalIdentifiersRepositoryItemReader} therefore it is marked as unused from the IDE.
   *
   * @param executionId The ID of the execution.
   * @param pageable Pagination and sorting configuration for the query.
   * @return A paginated list of ExecutionRecordExternalIdentifier entities matching the execution ID.
   */
  Page<ExecutionRecordExternalIdentifier> findByExecutionRun_ExecutionId(String executionId, Pageable pageable);

  /**
   * Counts the total number of entities associated with a specific execution ID.
   *
   * @param executionId The unique identifier of the execution run.
   * @return The count of matching entries.
   */
  long countByExecutionRun_ExecutionId(String executionId);

  /**
   * Removes all records associated with the specified dataset ID from the repository.
   *
   * @param datasetId The ID of the dataset whose associated records should be removed.
   */
  void removeByExecutionRun_DatasetId(String datasetId);

}
