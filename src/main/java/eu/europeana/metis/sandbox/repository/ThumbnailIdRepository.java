package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.ThumbnailIdEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThumbnailIdRepository extends JpaRepository<ThumbnailIdEntity, Long> {

  /**
   * Get thumbnails that belong to the given dataset id
   *
   * @param datasetId must not be null
   * @return list of thumbnails
   * @see <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation">Query Creation</a>
   */
  List<ThumbnailIdEntity> findByDatasetId(String datasetId);

  /**
   * Delete thumbnails that belong to the given dataset id
   *
   * @param datasetId must not be null
   */
  void deleteByDatasetId(String datasetId);
}
