package eu.europeana.metis.sandbox.service.metrics;

public interface MetricsService {

  DatasetMetrics datasetMetrics();

  void processMetrics(String datasetId);
}
