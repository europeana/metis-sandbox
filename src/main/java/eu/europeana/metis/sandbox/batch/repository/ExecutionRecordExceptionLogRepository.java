package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordException;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordExceptionLogRepository extends JpaRepository<ExecutionRecordException, ExecutionRecordIdentifier> {
  List<ExecutionRecordException> findByIdentifier_DatasetIdAndIdentifier_ExecutionName(String datasetId, String executionName);
  ExecutionRecordException findByIdentifier_DatasetIdAndIdentifier_RecordIdAndIdentifier_ExecutionName(String datasetId, String recordId, String executionName);
  long countByIdentifier_DatasetIdAndIdentifier_ExecutionName(String datasetId, String executionName);
}
