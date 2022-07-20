package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitle;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitleCompositeKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface RecordTitleRepository extends JpaRepository<RecordTitle, RecordTitleCompositeKey> {

  List<RecordTitle> findAllByExecutionPoint(ExecutionPoint executionPoint);

  @Modifying
  void deleteByExecutionPoint(ExecutionPoint executionPoint);
  @Modifying
  void deleteByExecutionPointDatasetId(String datasetId);
}
