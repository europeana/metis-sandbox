package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.DatasetEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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
  List<DatasetIdProjection> findByCreatedDateBefore(Instant date);

  /**
   * Retrieves a DatasetEntity based on the specified dataset ID.
   *
   * @param datasetId the ID of the dataset to retrieve
   * @return an Optional containing the found DatasetEntity or empty if no dataset matches the ID
   */
  Optional<DatasetEntity> findByDatasetId(int datasetId);

  /**
   * Retrieves a list of DatasetEntity objects that were created by the specified user.
   *
   * @param userId the ID of the user who created the datasets
   * @return a list of DatasetEntity objects created by the given user
   */
  List<DatasetEntity> findAllByCreatedById(String userId);

  /**
   * Updates the recordLimitExceeded field for a given dataset.
   *
   * @param datasetId the ID of the dataset
   */
  @Modifying
  @Transactional
  @Query("UPDATE DatasetEntity d SET d.recordLimitExceeded = true WHERE d.datasetId = :datasetId")
  void updateRecordLimitExceeded(@Param("datasetId") int datasetId);

  /**
   * Projection interface for accessing the dataset ID in queries.
   *
   * <p>This projection can be used to fetch only the dataset ID instead of whole dataset entities for optimized queries.
   */
  interface DatasetIdProjection {

    Integer getDatasetId();
  }
}
