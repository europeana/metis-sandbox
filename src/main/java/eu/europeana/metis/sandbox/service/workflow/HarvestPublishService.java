package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.utils.CompressedFileExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

public interface HarvestPublishService {

    /**
     * Start the harvest of a compressed file asynchronously on the given file {@link MultipartFile}
     *
     * @param file                    compressed file containing one or more records
     * @param datasetMetadata         object that encapsulates all data related to the dataset
     * @param compressedFileExtension the content type of file being uploaded
     * @return A HarvestContent object containing the content of the harvest and a bollean indicating
     * if it reached the max number of records
     * @throws ServiceException if file is not valid, error reading file, if records are empty
     */
    CompletableFuture<Void> runHarvestProvidedFileAsync(MultipartFile file, DatasetMetadata datasetMetadata, CompressedFileExtension compressedFileExtension);

    /**
     * Start the harvest of an url asynchronously on the given URL {@link String}
     *
     * @param url                     URL for compressed file containing one or more records
     * @param datasetMetadata         the object that encapsulates all data related to the dataset
     * @param compressedFileExtension the content type of the file being uploaded
     * @return A HarvestContent object containing the content of the harvest and a boolean indicating
     * if it reached the max number of records
     * @throws ServiceException if error processing URL, if URL timeout, if records are empty
     */
    CompletableFuture<Void> runHarvestHttpFileAsync(String url, DatasetMetadata datasetMetadata, CompressedFileExtension compressedFileExtension);

    /**
     * Async publish to message broker for further processing. This will send messages to 'harvestOai`
     * queue
     *
     * @param datasetMetadata the objevct that encapsulates all dataset related to the dataset
     * @param oaiHarvestData  And object that encapsulates the data necessary for OAI-PMH harvesting
     * @return {@link CompletableFuture} of the process
     */
    CompletableFuture<Void> runHarvestOaiPmhAsync(DatasetMetadata datasetMetadata, OaiHarvestData oaiHarvestData);
}
