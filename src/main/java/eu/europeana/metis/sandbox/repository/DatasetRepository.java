package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.service.dataset.projection.DatasetIdView;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<DatasetEntity, Integer> {

  List<DatasetIdView> getByCreatedDateBefore(LocalDateTime date);

  void deleteByDatasetIdIn(List<Integer> datasetId);
}
