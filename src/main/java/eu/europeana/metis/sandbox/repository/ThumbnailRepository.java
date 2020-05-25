package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.ThumbnailEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ThumbnailRepository extends JpaRepository<ThumbnailEntity, Long> {

  /**
   * Get thumbnails that belong to the given dataset id
   *
   * @param datasetId must not be null
   * @return list of thumbnails
   */
  List<ThumbnailEntity> findByDatasetId(String datasetId);

  /**
   * Delete thumbnails that belong to the given dataset id
   *
   * @param datasetId must not be null
   */
  @Modifying
  @Query("delete from ThumbnailEntity where datasetId = ?1")
  void deleteByDatasetId(String datasetId);
}
