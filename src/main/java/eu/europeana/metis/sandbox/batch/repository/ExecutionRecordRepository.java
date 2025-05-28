package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, ExecutionRecordIdentifier> {
  Page<ExecutionRecord> findByIdentifier_DatasetIdAndIdentifier_ExecutionId(String datasetId, String executionId, Pageable pageable);
  ExecutionRecord findByIdentifier_DatasetIdAndIdentifier_ExecutionName(String datasetId, String executionName);
  ExecutionRecord findByIdentifier_DatasetIdAndIdentifier_RecordIdAndIdentifier_ExecutionName(String datasetId, String recordId, String executionName);
  long countByIdentifier_DatasetIdAndIdentifier_ExecutionName(String datasetId, String executionName);
}
