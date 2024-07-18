package eu.europeana.metis.sandbox.repository.debias;

import eu.europeana.metis.sandbox.entity.debias.DetectionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DetectRepository extends JpaRepository<DetectionEntity, Long> {

  @Query("SELECT dec.datasetId, dec.state, dec.createdDate FROM DetectionEntity dec")
  List<DetectionEntity> listDbEntries();

  DetectionEntity findDetectionEntityByDatasetId_DatasetId(Long datasetId);

  @Modifying
  @Query("UPDATE DetectionEntity dec SET dec.state = '?2' WHERE dec.datasetId = ?1")
  void updateState(Long datasetId, String state);

}
