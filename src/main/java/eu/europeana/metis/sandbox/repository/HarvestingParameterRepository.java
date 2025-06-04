package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.HarvestParametersEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository connected to Harvesting Parameters table
 */
public interface HarvestingParameterRepository extends JpaRepository<HarvestParametersEntity, UUID> {

    Optional<HarvestParametersEntity> findById(UUID id);

    /**
     * Gets the entity based on the dataset id
     * @param datasetId The id of the dataset to do the search
     * @return The entity associated to the dataset id
     */
    HarvestParametersEntity getHarvestingParametersEntitiesByDatasetId_DatasetId(Integer datasetId);

    /**
     * Removes the entity from the table based on the dataset id
     * @param datasetId The dataset id associated to the entity
     */
    @Modifying
    @Query("DELETE FROM HarvestParametersEntity hpe WHERE EXISTS (SELECT 1 FROM DatasetEntity dte "
        + "WHERE dte.datasetId = hpe.datasetId.datasetId AND hpe.datasetId.datasetId = ?1)")
    void deleteByDatasetIdDatasetId(Integer datasetId);
}
