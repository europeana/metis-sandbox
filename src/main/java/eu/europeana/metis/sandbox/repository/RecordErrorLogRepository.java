package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.RecordErrorLogEntity;
import eu.europeana.metis.sandbox.repository.projection.ErrorLogView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RecordErrorLogRepository extends JpaRepository<RecordErrorLogEntity, Long> {

  List<ErrorLogView> getByDatasetId(String datasetId);

  @Modifying
  @Query("delete from RecordErrorLogEntity where datasetId = ?1")
  void deleteByDatasetId(String datasetId);
}
