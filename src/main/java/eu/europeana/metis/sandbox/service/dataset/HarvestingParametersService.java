package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.Protocol;
import eu.europeana.metis.sandbox.entity.HarvestingParametersEntity;

public interface HarvestingParametersService {

    HarvestingParametersEntity createDatasetHarvestingParameters(String datasetId, Protocol protocol, String fileName,
                                                                 String fileType, String url, String setSpec, String metadataFormat);

    HarvestingParametersEntity getDatasetHarvestingParameters(String datasetId);

    void remove(String datasetId);
}
