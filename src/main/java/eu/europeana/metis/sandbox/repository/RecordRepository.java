package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordRepository extends JpaRepository<RecordEntity, Long> {

  List<RecordEntity> getByDatasetIdAndStatus(String datasetId, Status status);
}
