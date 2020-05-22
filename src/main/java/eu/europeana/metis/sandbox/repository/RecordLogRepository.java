package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.entity.StepStatistic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

  @Modifying
  @Query("delete from RecordLogEntity where datasetId = ?1")
  void deleteByDatasetId(String datasetId);
}
