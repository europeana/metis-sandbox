package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.DatasetLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetLogRepository extends JpaRepository<DatasetLogEntity, Long> {

  List<DatasetLogEntity> findByDatasetDatasetId(int datasetId);

}
