package eu.europeana.metis.sandbox.service.metrics;

import eu.europeana.metis.sandbox.common.Step;

public interface MetricsService {
   DatasetMetrics datasetMetrics();
   void processMetrics(String datasetId, Step step);
}
