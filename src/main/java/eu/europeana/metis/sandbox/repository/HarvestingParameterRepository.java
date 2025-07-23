package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.HarvestingParameterEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository connected to Harvesting Parameters table
 */
public interface HarvestingParameterRepository extends JpaRepository<HarvestingParameterEntity, Long> {

  /**
   * Gets the entity based on the dataset id
   *
   * @param datasetId The id of the dataset to do the search
   * @return The entity associated to the dataset id
   */
  HarvestingParameterEntity getHarvestingParametersEntitiesByDatasetId_DatasetId(Integer datasetId);

  /**
   * Gets harvesting parameter entities by dataset id created by id.
   *
   * @param userId the user id
   * @return the harvesting parameter entities by dataset id created by id
   */
  List<HarvestingParameterEntity> getHarvestingParameterEntitiesByDatasetId_CreatedById(String userId);

  /**
   * Removes the entity from the table based on the dataset id
   *
   * @param datasetId The dataset id associated to the entity
   */
  @Modifying
  @Query("DELETE FROM HarvestingParameterEntity hpe WHERE EXISTS (SELECT 1 FROM DatasetEntity dte "
      + "WHERE dte.datasetId = hpe.datasetId.datasetId AND hpe.datasetId.datasetId = ?1)")
  void deleteByDatasetIdDatasetId(Integer datasetId);
}
