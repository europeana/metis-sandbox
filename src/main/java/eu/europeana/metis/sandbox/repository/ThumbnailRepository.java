package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.ThumbnailEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ThumbnailRepository extends JpaRepository<ThumbnailEntity, Long> {

  List<ThumbnailEntity> findByDatasetId(String datasetId);

  @Modifying
  @Query("delete from ThumbnailEntity where datasetId = ?1")
  void deleteByDatasetId(String datasetId);
}
