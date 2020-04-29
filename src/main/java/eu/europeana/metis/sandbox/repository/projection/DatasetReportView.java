package eu.europeana.metis.sandbox.repository.projection;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;

public interface DatasetReportView {

  Long getId();

  String getRecordId();

  String getDatasetId();

  Step getStep();

  Status getStatus();

  String getMessage();
}
