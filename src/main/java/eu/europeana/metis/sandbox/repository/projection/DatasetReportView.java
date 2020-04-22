package eu.europeana.metis.sandbox.repository.projection;

import eu.europeana.metis.sandbox.common.Step;

public interface DatasetReportView {

  String getError();

  ReportKey getKey();

  interface ReportKey {

    String getId();

    String getDatasetId();

    Step getStep();
  }
}
