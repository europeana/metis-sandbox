package eu.europeana.metis.sandbox.entity.projection;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.common.Status;

public interface ErrorLogView {

  Long getId();

  String getRecordId();

  FullBatchJobType getStep();

  Status getStatus();

  String getMessage();
}
