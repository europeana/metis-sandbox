package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.Protocol;
import eu.europeana.metis.sandbox.entity.HarvestingParametersEntity;

/**
 * Interface class of HarvestingParameters service
 */
public interface HarvestingParametersService {

    /**
     * Creates a new entry into the database
     *
     * @param datasetId      The id of the dataset associated to the harvesting parameters
     * @param protocol       The type of harvesting done in the given dataset
     * @param fileName       The file name of the file used for harvesting (if it exists)
     * @param fileType       The type of file used for harvesting e.g., zip, tar (if it exists)
     * @param url            The url used for harvesting (if it exists)
     * @param setSpec        The setsepc used for harvesting (if it exists)
     * @param metadataFormat The metadata format used for harvesting (if it exists)
     */
    void createDatasetHarvestingParameters(String datasetId, Protocol protocol, String fileName,
                                           String fileType, String url, String setSpec, String metadataFormat);

    /**
     * Returns the entity based on the dataset id
     * @param datasetId The id of the dataset
     * @return The entity associated to the given dataset id
     */
    HarvestingParametersEntity getDatasetHarvestingParameters(String datasetId);

    /**
     * Removed the entity from the database
     * @param datasetId The id of the dataset
     */
    void remove(String datasetId);
}
