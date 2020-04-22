package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntityKey;
import eu.europeana.metis.sandbox.repository.projection.DatasetReportView;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordLogRepository extends JpaRepository<RecordLogEntity, RecordLogEntityKey> {

  List<DatasetReportView> getByKeyDatasetIdAndResult(String datasetId, Status result);
}
