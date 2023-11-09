package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.dto.HarvestingParametricDto;
import eu.europeana.metis.sandbox.entity.HarvestingParameterEntity;

/**
 * Interface class of HarvestingParameters service
 */
public interface HarvestingParameterService {

    /**
     * Creates a new entry into the database
     *
     * @param datasetId      The id of the dataset associated to the harvesting parameters
     * @param harvestingParametricDto Object encapsulating data related to the harvesting related to the dataset
     */
    void createDatasetHarvestingParameters(String datasetId, HarvestingParametricDto harvestingParametricDto);

    /**
     * Returns the entity based on the dataset id
     * @param datasetId The id of the dataset
     * @return The entity associated to the given dataset id
     */
    HarvestingParameterEntity getDatasetHarvestingParameters(String datasetId);

    /**
     * Removed the entity from the database
     * @param datasetId The id of the dataset
     */
    void remove(String datasetId);
}
