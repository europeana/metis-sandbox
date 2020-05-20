package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.ThumbnailEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThumbnailRepository extends JpaRepository<ThumbnailEntity, Long> {

  List<ThumbnailEntity> findByDatasetIdIn(List<String> datasetIds);

  void deleteByDatasetIdIn(List<String> datasetIds);
}
