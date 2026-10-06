package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.ThumbnailIdEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing thumbnail IDs associated with datasets.
 */
public interface ThumbnailIdRepository extends JpaRepository<ThumbnailIdEntity, Long> {

  /**
   * Get thumbnails that belong to the given dataset id.
   *
   * @param datasetId the dataset id
   * @return list of thumbnail identifier entities
   */
  List<ThumbnailIdEntity> findByDatasetId(String datasetId);

  /**
   * Delete thumbnails that belong to the given dataset id.
   *
   * @param datasetId the dataset id
   */
  void deleteByDatasetId(String datasetId);
}
