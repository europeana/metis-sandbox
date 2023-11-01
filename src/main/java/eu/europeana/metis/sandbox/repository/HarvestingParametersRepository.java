package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.HarvestingParametersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository connected to Harvesting Parameters table
 */
public interface HarvestingParametersRepository extends JpaRepository<HarvestingParametersEntity, Long> {

    /**
     * Gets the entity based on the dataset id
     * @param datasetId The id of the dataset to do the search
     * @return The entity associated to the dataset id
     */
    HarvestingParametersEntity getHarvestingParametersEntitiesByDatasetId_DatasetId(Integer datasetId);

    /**
     * Removes the entity from the table based on the dataset id
     * @param datasetId The dataset id associated to the entity
     */
    void deleteByDatasetId_DatasetId(Integer datasetId);
}
