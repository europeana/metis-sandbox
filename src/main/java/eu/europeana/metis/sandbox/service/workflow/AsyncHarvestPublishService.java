package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;

import java.util.concurrent.CompletableFuture;

public interface AsyncHarvestPublishService {

    /**
     * Async publish to message broker for further processing. This will send messages to 'harvestOai`
     * queue
     *
     * @param datasetName    The name of the dataset to be harvested
     * @param datasetId      The id of the dataset to be harvested
     * @param country        The country of the dataset to be harvested
     * @param language       The language of the dataset to be harvested
     * @param oaiHarvestData And object that encapsulates the data necessary for OAI-PMH harvesting
     * @return {@link CompletableFuture} of the process
     */
    CompletableFuture<Void> runHarvestOaiAsync(String datasetName, String datasetId, Country country,
                                               Language language, OaiHarvestData oaiHarvestData);
}
