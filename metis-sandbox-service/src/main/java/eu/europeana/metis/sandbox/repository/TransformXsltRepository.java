package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.XsltType;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing Transform XSLT entities.
 */
public interface TransformXsltRepository extends JpaRepository<TransformXsltEntity, Integer> {

  Optional<TransformXsltEntity> findById(@NotNull Integer id);

  /**
   * Retrieves the first TransformXsltEntity of the specified type, ordered by ID.
   *
   * @param type the XSLT type to filter by
   * @return an Optional containing the first matching TransformXsltEntity, or empty if none exist
   */
  Optional<TransformXsltEntity> findFirstByTypeOrderById(XsltType type);

  /**
   * Retrieves a TransformXsltEntity associated with the given dataset ID.
   *
   * @param datasetId the identifier of the dataset linked to the XSLT transformation
   * @return an Optional containing the matching TransformXsltEntity, or empty if not found
   */
  Optional<TransformXsltEntity> findByDatasetId(String datasetId);
}
