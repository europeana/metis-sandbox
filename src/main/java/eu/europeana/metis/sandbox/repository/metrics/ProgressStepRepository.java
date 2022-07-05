package eu.europeana.metis.sandbox.repository.metrics;

import eu.europeana.metis.sandbox.entity.metrics.ProgressStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressStepRepository extends JpaRepository<ProgressStepEntity, Integer> {

  ProgressStepEntity findByDatasetIdAndStep(String datasetId, String step);
}
