package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.entity.DatasetLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetLogRepository extends JpaRepository<DatasetLogEntity, Long> {

  List<DatasetLogEntity> findByDatasetDatasetId(int datasetId);

  List<DatasetLogEntity> findByDatasetDatasetIdAndStatus(int datasetId, Status status);
}
