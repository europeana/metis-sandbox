package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.service.dataset.projection.DatasetIdView;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DatasetRepository extends JpaRepository<DatasetEntity, Integer> {

  List<DatasetIdView> getByCreatedDateBefore(LocalDateTime date);

  @Modifying
  @Query("delete from DatasetEntity where datasetId = ?1")
  void deleteByDatasetId(Integer datasetId);
}
