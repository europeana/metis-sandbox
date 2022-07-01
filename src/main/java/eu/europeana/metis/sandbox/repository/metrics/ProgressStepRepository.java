package eu.europeana.metis.sandbox.repository.metrics;

import eu.europeana.metis.sandbox.entity.metrics.ProgressStep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressStepRepository extends JpaRepository<ProgressStep, Integer> {

  ProgressStep findByDatasetIdAndStep(String datasetId, String step);
}
