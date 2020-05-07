package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.projection.RecordLogView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordLogRepository extends JpaRepository<RecordLogEntity, Long> {

  List<RecordLogView> getByDatasetId(String datasetId);
}
