package eu.europeana.metis.sandbox.repository.projection;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;

public interface RecordLogView {

  String getRecordId();

  Step getStep();

  Status getStatus();
}
