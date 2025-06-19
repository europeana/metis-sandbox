package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.DatasetEntity;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for accessing and performing operations on datasets.
 */
public interface DatasetRepository extends JpaRepository<DatasetEntity, Integer> {

  /**
   * Returns a list of DatasetIdProjection objects created before the specified date.
   *
   * @param date the date to compare against
   * @return a list of DatasetIdProjection objects matching the criteria
   */
  List<DatasetIdProjection> findByCreatedDateBefore(ZonedDateTime date);

  /**
   * Retrieves a DatasetEntity based on the specified dataset ID.
   *
   * @param datasetId the ID of the dataset to retrieve
   * @return an Optional containing the found DatasetEntity or empty if no dataset matches the ID
   */
  Optional<DatasetEntity> findByDatasetId(int datasetId);

  /**
   * Projection interface for accessing the dataset ID in queries.
   *
   * <p>This projection can be used to fetch only the dataset ID instead of whole dataset entities for optimized queries.
   */
  interface DatasetIdProjection {

    Integer getDatasetId();
  }
}
