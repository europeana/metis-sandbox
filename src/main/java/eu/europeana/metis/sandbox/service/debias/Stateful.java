package eu.europeana.metis.sandbox.service.debias;

public interface Stateful {
  void fail(Long datasetId);
  void success(Long datasetId);
  boolean process(Long datasetId);
}
