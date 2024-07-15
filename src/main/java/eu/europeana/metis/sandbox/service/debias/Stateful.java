package eu.europeana.metis.sandbox.service.debias;

public interface Stateful {
  void fail(String datasetId);
  void success(String datasetId);
  boolean process(String datasetId);
}
