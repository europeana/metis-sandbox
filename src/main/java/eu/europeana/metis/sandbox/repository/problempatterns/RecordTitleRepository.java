package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitle;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitleCompositeKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * The interface Record title repository.
 */
public interface RecordTitleRepository extends JpaRepository<RecordTitle, RecordTitleCompositeKey> {

  /**
   * Find all by execution point list.
   *
   * @param executionPoint the execution point
   * @return the list
   */
  List<RecordTitle> findAllByExecutionPoint(ExecutionPoint executionPoint);

  /**
   * Delete by execution point.
   *
   * @param executionPoint the execution point
   */
  @Modifying
  void deleteByExecutionPoint(ExecutionPoint executionPoint);

  /**
   * Delete by execution point dataset id.
   *
   * @param datasetId the dataset id
   */
  @Modifying
  @Query("DELETE FROM RecordTitle rti WHERE EXISTS (SELECT 1 FROM ExecutionPoint ep "
      + "WHERE rti.executionPoint.executionPointId = ep.executionPointId AND ep.datasetId = ?1)")
  void deleteByExecutionPointDatasetId(String datasetId);
}
