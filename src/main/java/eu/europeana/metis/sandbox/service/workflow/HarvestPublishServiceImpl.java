package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.Protocol;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.sandbox.domain.Record;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import eu.europeana.metis.sandbox.service.dataset.HarvestingParametersService;
import eu.europeana.metis.utils.CompressedFileExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class HarvestPublishServiceImpl implements HarvestPublishService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestPublishServiceImpl.class);
    private static final String HARVESTING_ERROR_MESSAGE = "Error harvesting records for dataset: ";

    private final HarvestService harvestService;
    private final Executor asyncServiceTaskExecutor;
    private final HarvestingParametersService harvestingParametersService;

    public HarvestPublishServiceImpl(HarvestService harvestService,
                                     Executor asyncServiceTaskExecutor,
                                     HarvestingParametersService harvestingParametersService) {
        this.harvestService = harvestService;
        this.asyncServiceTaskExecutor = asyncServiceTaskExecutor;
        this.harvestingParametersService = harvestingParametersService;
    }

    @Override
    public CompletableFuture<Void> runHarvestFileAsync(MultipartFile file, DatasetMetadata datasetMetadata,
                                                       CompressedFileExtension compressedFileExtension) {
        try {
            Record.RecordBuilder recordDataEncapsulated = Record.builder()
                    .datasetId(datasetMetadata.getDatasetId())
                    .datasetName(datasetMetadata.getDatasetName())
                    .country(datasetMetadata.getCountry())
                    .language(datasetMetadata.getLanguage());
            harvestingParametersService.createDatasetHarvestingParameters(datasetMetadata.getDatasetId(),
                    Protocol.FILE, file.getName(), compressedFileExtension.name(), null, null, null);
            return runHarvestFileAsync(file.getInputStream(), recordDataEncapsulated, datasetMetadata, compressedFileExtension);
        } catch (IOException e) {
            throw new ServiceException("Error harvesting records from file " + file.getName(), e);
        }
    }

    @Override
    public CompletableFuture<Void> runHarvestHttpFileAsync(String url, DatasetMetadata datasetMetadata,
                                                           CompressedFileExtension compressedFileExtension) {
        Record.RecordBuilder recordDataEncapsulated = Record.builder()
                .datasetId(datasetMetadata.getDatasetId())
                .datasetName(datasetMetadata.getDatasetName())
                .country(datasetMetadata.getCountry())
                .language(datasetMetadata.getLanguage());
        harvestingParametersService.createDatasetHarvestingParameters(datasetMetadata.getDatasetId(), Protocol.HTTP,
                null, null, url, null, null);
        return CompletableFuture.runAsync(() -> {
            try (InputStream input = new URL(url).openStream()) {
                harvestService.harvest(input, datasetMetadata.getDatasetId(), recordDataEncapsulated,
                        datasetMetadata.getStepSize(), compressedFileExtension);
            } catch (UnknownHostException e) {
                throw new ServiceException(HARVESTING_ERROR_MESSAGE + datasetMetadata.getDatasetId()
                        + " - unknown host: " + e.getMessage());
            } catch (IOException | HarvesterException e) {
                throw new ServiceException(HARVESTING_ERROR_MESSAGE + datasetMetadata.getDatasetId(), e);
            }
        }, asyncServiceTaskExecutor);
    }

    private CompletableFuture<Void> runHarvestFileAsync(InputStream inputStreamToHarvest,
                                                        Record.RecordBuilder recordDataEncapsulated,
                                                        DatasetMetadata datasetMetadata,
                                                        CompressedFileExtension compressedFileExtension) {
        return CompletableFuture.runAsync(() -> {
            try {
                harvestService.harvest(inputStreamToHarvest, datasetMetadata.getDatasetId(), recordDataEncapsulated,
                        datasetMetadata.getStepSize(), compressedFileExtension);
            } catch (HarvesterException e) {
                throw new ServiceException(HARVESTING_ERROR_MESSAGE + datasetMetadata.getDatasetId(), e);
            }
        }, asyncServiceTaskExecutor).whenComplete((result, exception) -> {
            try {
                inputStreamToHarvest.close();
            } catch (IOException e) {
                LOGGER.warn("Could not close input stream", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> runHarvestOaiPmhAsync(DatasetMetadata datasetMetadata,
                                                         OaiHarvestData oaiHarvestData) {
        Record.RecordBuilder recordDataEncapsulated = Record.builder()
                .country(datasetMetadata.getCountry())
                .language(datasetMetadata.getLanguage())
                .datasetName(datasetMetadata.getDatasetName())
                .datasetId(datasetMetadata.getDatasetId());
        harvestingParametersService.createDatasetHarvestingParameters(datasetMetadata.getDatasetId(), Protocol.OAI_PMH,
                null, null, oaiHarvestData.getUrl(), oaiHarvestData.getSetspec(), oaiHarvestData.getMetadataformat());
        return CompletableFuture.runAsync(
                () -> harvestService.harvestOaiPmh(datasetMetadata.getDatasetId(), recordDataEncapsulated, oaiHarvestData,
                        datasetMetadata.getStepSize()), asyncServiceTaskExecutor);
    }
}
