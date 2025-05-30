package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, ExecutionRecordIdentifier> {
  Page<ExecutionRecord> findByIdentifier_DatasetIdAndIdentifier_ExecutionId(String datasetId, String executionId, Pageable pageable);
  ExecutionRecord findByIdentifier_DatasetIdAndIdentifier_ExecutionName(String datasetId, String executionName);
  ExecutionRecord findByIdentifier_DatasetIdAndIdentifier_RecordIdAndIdentifier_ExecutionName(String datasetId, String recordId, String executionName);
  Set<ExecutionRecord> findByIdentifier_DatasetIdAndIdentifier_RecordIdAndIdentifier_ExecutionNameIn(String datasetId, String recordId, Set<String> executionName);
  long countByIdentifier_DatasetIdAndIdentifier_ExecutionName(String datasetId, String executionName);

  @Query("SELECT r.identifier.datasetId AS datasetId, COUNT(r) AS count " +
      "FROM ExecutionRecord r GROUP BY r.identifier.datasetId")
  List<DatasetStatisticProjection> getDatasetStatistics();

  @Query("SELECT r.identifier.executionName AS step, COUNT(r) AS count " +
      "FROM ExecutionRecord r " +
      "GROUP BY r.identifier.executionName")
  List<StepStatisticProjection> getStepStatistics();

  interface DatasetStatisticProjection {
    String getDatasetId();
    long getCount();
  }

  interface StepStatisticProjection {
    String getStep();
    long getCount();
  }

  void removeByIdentifier_DatasetId(String datasetId);
}
