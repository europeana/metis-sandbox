package eu.europeana.metis.sandbox.repository.metrics;

import eu.europeana.metis.sandbox.entity.metrics.ProgressDataset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressDatasetRepository extends JpaRepository<ProgressDataset, Integer> {

  ProgressDataset findByDatasetId(String datasetId);
}
