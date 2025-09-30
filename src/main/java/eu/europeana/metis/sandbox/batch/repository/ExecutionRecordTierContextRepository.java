package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link ExecutionRecordTierContext} entities.
 */
@Repository
public interface ExecutionRecordTierContextRepository extends
    JpaRepository<ExecutionRecordTierContext, ExecutionRecordIdentifierKey> {

  /**
   * Retrieves a list of ExecutionRecordTierContext entities based on the dataset ID.
   *
   * @param datasetId The ID of the dataset.
   * @return A list of ExecutionRecordTierContext entities matching the dataset ID.
   */
  List<ExecutionRecordTierContext> findByIdentifier_DatasetId(String datasetId);

  /**
   * Retrieves the top 10 ExecutionRecordTierContext entities based on the given dataset ID and content tier.
   *
   * @param datasetId The ID of the dataset.
   * @param contentTier The content tier.
   * @return A list of up to 10 ExecutionRecordTierContext entities matching the criteria.
   */
  List<ExecutionRecordTierContext> findTop10ByIdentifier_DatasetIdAndContentTier(String datasetId, String contentTier);

  /**
   * Retrieves the top 10 execution record tier contexts based on the given dataset identifier and metadata tier.
   *
   * @param datasetId The ID of the dataset.
   * @param metadataTier The metadata tier.
   * @return A list of up to 10 execution record tier contexts matching the specified dataset ID and metadata tier.
   */
  List<ExecutionRecordTierContext> findTop10ByIdentifier_DatasetIdAndMetadataTier(String datasetId, String metadataTier);

  /**
   * Counts the number of ExecutionRecordTierContext entities matching the given dataset ID and content tier.
   *
   * @param datasetId The ID of the dataset.
   * @param contentTier The content tier.
   * @return The count of records matching the specified dataset ID and content tier.
   */
  long countByIdentifier_DatasetIdAndContentTier(String datasetId, String contentTier);

  /**
   * Counts the number of ExecutionRecordTierContext entities matching the given dataset ID and metadata tier.
   *
   * @param datasetId The ID of the dataset.
   * @param metadataTier The metadata tier.
   * @return The count of records matching the specified dataset ID and metadata tier.
   */
  long countByIdentifier_DatasetIdAndMetadataTier(String datasetId, String metadataTier);

  /**
   * Removes all ExecutionRecordTierContext entities that match the specified dataset ID.
   *
   * @param datasetId The ID of the dataset.
   */
  void removeByIdentifier_DatasetId(String datasetId);
}

