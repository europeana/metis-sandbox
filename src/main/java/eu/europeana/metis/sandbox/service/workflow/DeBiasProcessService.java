package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.service.workflow.DeBiasProcessServiceImpl.DeBiasInputRecord;
import eu.europeana.metis.sandbox.service.workflow.DeBiasProcessServiceImpl.DeBiasReportRow;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * The interface De Bias process service.
 */
public interface DeBiasProcessService {

  List<DeBiasInputRecord> getDeBiasSourceFieldsFromRecordsNew(String recordContent, String recordId);

  @Transactional
  void process(String recordContent, String datasetId, String recordId);

  List<DeBiasReportRow> doDeBiasAndGenerateReportNew(List<DeBiasInputRecord> deBiasInputRecords);
}
