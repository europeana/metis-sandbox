package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.projection.DatasetIdView;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<DatasetEntity, Integer> {

  /**
   * Get a list of datasets created before specified date
   *
   * @param date must not be null
   * @return list of dataset ids
   * @see DatasetIdView
   */
  List<DatasetIdView> getByCreatedDateBefore(LocalDateTime date);
}
