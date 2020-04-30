package eu.europeana.metis.sandbox.repository.projection;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;

public interface ErrorLogView {

  Long getId();

  String getRecordId();

  Integer getDatasetId();

  Step getStep();

  Status getStatus();

  String getMessage();
}
