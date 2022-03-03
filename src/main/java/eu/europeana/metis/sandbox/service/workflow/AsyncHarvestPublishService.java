package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

public interface AsyncHarvestPublishService {

    /**
     * Start the harvest of a zip asynchronously on the given file {@link MultipartFile}
     *
     * @param file zip file containing one or more records
     * @param datasetName The name of the dataset to harvest
     * @param datasetId The id of the dataset to harvest
     * @param country The country of the dataset to be harvested
     * @param language The language of the dataset to be harvested
     * @return A HarvestContent object containing the content of the harvest and a bollean indicating
     * if it reached the max number of records
     * @throws ServiceException if file is not valid, error reading file, if records are empty
     */
    CompletableFuture<Void> runZipHarvestAsync(MultipartFile file, String datasetName, String datasetId, Country country, Language language);

    /**
     * Start the harvest of an url asynchronously on the given URL {@link String}
     *
     * @param url URL for zip file containing one or more records
     * @param datasetName The name of the dataset to harvest
     * @param datasetId The id of the dataset to harvest
     * @param country The country of the dataset to be harvested
     * @param language The language of the dataset to be harvested
     * @return A HarvestContent object containing the content of the harvest and a boolean indicating
     * if it reached the max number of records
     * @throws ServiceException if error processing URL, if URL timeout, if records are empty
     */
    CompletableFuture<Void> runHttpHarvestAsync(String url, String datasetName, String datasetId, Country country, Language language);

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
