package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.RecordErrorLogEntity;
import eu.europeana.metis.sandbox.repository.projection.ErrorLogView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordErrorLogRepository extends JpaRepository<RecordErrorLogEntity, Long> {

  List<ErrorLogView> getByDatasetId(String datasetId);

  void deleteByDatasetId(String datasetId);
}
