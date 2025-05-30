package eu.europeana.metis.sandbox.entity.projection;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;

public interface ErrorLogView {

  Long getId();

  String getRecordId();

  Step getStep();

  Status getStatus();

  String getMessage();
}
