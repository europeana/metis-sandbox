package eu.europeana.metis.sandbox.repository.projection;

import eu.europeana.metis.sandbox.common.Step;
import java.util.List;

public interface DatasetReportView {

  Long getId();
  String getRecordId();
  String getDatasetId();
  Step getStep();
  List<RecordErrorView> getRecordErrors();
}
