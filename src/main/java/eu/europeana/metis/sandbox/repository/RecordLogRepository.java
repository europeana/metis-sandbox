package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.entity.StepStatistic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecordLogRepository extends JpaRepository<RecordLogEntity, Long> {

  @Query("SELECT " +
      "    new eu.europeana.metis.sandbox.entity.StepStatistic(rle.step, rle.status, COUNT(rle)) " +
      "FROM " +
      "    RecordLogEntity rle " +
      "WHERE rle.datasetId = ?1 " +
      "GROUP BY " +
      "    rle.step, rle.status")
  List<StepStatistic> getStepStatistics(String datasetId);

  void deleteByDatasetIdIn(List<String> datasetId);
}
