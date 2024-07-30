package eu.europeana.metis.sandbox.service.debias;

public interface Stateful {
  void fail(Integer datasetId);
  void success(Integer datasetId);
  boolean process(Integer datasetId);
}
