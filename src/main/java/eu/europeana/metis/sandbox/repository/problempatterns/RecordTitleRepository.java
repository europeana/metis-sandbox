package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitle;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitleCompositeKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * The interface Record title repository.
 */
@Repository
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
  @Query("""
      DELETE FROM RecordTitle rti WHERE EXISTS (SELECT 1 FROM ExecutionPoint ep
            WHERE rti.executionPoint.executionPointId = ep.executionPointId AND ep.datasetId = :datasetId)
      """)
  void deleteByExecutionPointDatasetId(@Param("datasetId") String datasetId);


  @Modifying
  @Transactional
  @Query(value = """
        DELETE FROM problem_patterns.record_title rt 
        USING (
            SELECT x.execution_point_id, x.record_id, x.title 
            FROM problem_patterns.record_title x 
            INNER JOIN (
                SELECT MIN(record_id) AS record_id, UPPER(title) AS uppercase_title 
                FROM problem_patterns.record_title 
                WHERE execution_point_id = :executionPointId 
                GROUP BY UPPER(title) 
                HAVING COUNT(*) = 1
            ) y ON UPPER(x.title) = y.uppercase_title AND x.record_id = y.record_id 
            WHERE x.execution_point_id = :executionPointId
        ) sub 
        WHERE rt.execution_point_id = sub.execution_point_id 
        AND rt.record_id = sub.record_id 
        AND rt.title = sub.title
    """, nativeQuery = true)
  int deleteRedundantRecordTitles(@Param("executionPointId") int executionPointId);
}
