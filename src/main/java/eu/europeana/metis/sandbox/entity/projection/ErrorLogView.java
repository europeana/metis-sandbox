package eu.europeana.metis.sandbox.entity.projection;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.RecordEntity;

public interface ErrorLogView {

  Long getId();

  RecordEntity getRecordId();

  Step getStep();

  Status getStatus();

  String getMessage();
}
