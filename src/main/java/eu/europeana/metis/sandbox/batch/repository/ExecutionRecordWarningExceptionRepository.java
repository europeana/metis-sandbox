package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordWarningException;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordWarningExceptionRepository extends
    JpaRepository<ExecutionRecordWarningException, Long> {

  List<ExecutionRecordWarningException> findByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(String datasetId,
      String executionName);

  long countByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(String datasetId, String executionName);
}
