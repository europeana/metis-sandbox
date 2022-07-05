package eu.europeana.metis.sandbox.repository.metrics;

import eu.europeana.metis.sandbox.entity.metrics.ProgressDatasetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressDatasetRepository extends JpaRepository<ProgressDatasetEntity, Integer> {

  ProgressDatasetEntity findByDatasetId(String datasetId);
}
