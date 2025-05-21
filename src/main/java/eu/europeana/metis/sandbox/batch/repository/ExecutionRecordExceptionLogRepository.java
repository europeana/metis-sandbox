package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExceptionLog;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordExceptionLogRepository extends JpaRepository<ExecutionRecordExceptionLog, ExecutionRecordIdentifier> {
  List<ExecutionRecordExceptionLog> findByIdentifier_DatasetIdAndIdentifier_ExecutionName(String datasetId, String executionName);
  long countByIdentifier_DatasetIdAndIdentifier_ExecutionName(String datasetId, String executionName);
}
