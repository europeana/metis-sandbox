package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExceptionLog;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordExceptionLogRepository extends JpaRepository<ExecutionRecordExceptionLog, ExecutionRecordIdentifier> {
  Page<ExecutionRecordExceptionLog> findByIdentifier_DatasetIdAndExecutionName(String datasetId, String executionName, Pageable pageable);
  long countByIdentifier_DatasetIdAndIdentifier_ExecutionId(String datasetId, String executionId);
  long countByIdentifier_DatasetIdAndExecutionName(String datasetId, String executionName);
}
