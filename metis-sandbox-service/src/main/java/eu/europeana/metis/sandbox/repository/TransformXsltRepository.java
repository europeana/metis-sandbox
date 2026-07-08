package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.XsltType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing Transform XSLT entities.
 */
public interface TransformXsltRepository extends JpaRepository<TransformXsltEntity, Integer> {

  String DEFAULT_DATASET_ID = "-1";

  /**
   * Retrieves a TransformXsltEntity by its unique identifier.
   *
   * @param id the unique identifier of the TransformXsltEntity to be retrieved
   * @return an Optional containing the found TransformXsltEntity, or empty if no entity is found with the given ID
   */
  Optional<TransformXsltEntity> findById(Integer id);

  /**
   * Retrieves a TransformXsltEntity associated with the given dataset ID.
   *
   * @param datasetId the identifier of the dataset linked to the XSLT transformation
   * @return an Optional containing the matching TransformXsltEntity, or empty if not found
   */
  Optional<TransformXsltEntity> findByDatasetId(String datasetId);

  /**
   * Retrieves a TransformXsltEntity associated with the given dataset ID and XSLT type.
   *
   * @param datasetId the identifier of the dataset linked to the XSLT transformation
   * @param xsltType the XSLT type specifying the nature of the transformation
   * @return an Optional containing the matching TransformXsltEntity, or empty if no entity is found
   */
  Optional<TransformXsltEntity> findByDatasetIdAndType(String datasetId, XsltType xsltType);
}
