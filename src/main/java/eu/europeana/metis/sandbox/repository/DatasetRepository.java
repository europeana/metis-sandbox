package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.DatasetEntity;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<DatasetEntity, Integer> {

  /**
   * Get a list of datasets created before specified date
   *
   * @param date must not be null
   * @return list of dataset ids
   * @see DatasetIdProjection
   * @see <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation">Query
   * Creation</a>
   * @see <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#projections">Projections</a>
   */
  List<DatasetIdProjection> findByCreatedDateBefore(ZonedDateTime date);

  Optional<DatasetEntity> findByDatasetId(int datasetId);

  interface DatasetIdProjection {

    Integer getDatasetId();
  }
}
