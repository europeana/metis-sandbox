package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.sandbox.domain.Record;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class HarvestPublishServiceImpl implements HarvestPublishService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestPublishServiceImpl.class);

    private final HarvestService harvestService;
    private final Executor asyncServiceTaskExecutor;

    public HarvestPublishServiceImpl(HarvestService harvestService,
                                     Executor asyncServiceTaskExecutor) {
        this.harvestService = harvestService;
        this.asyncServiceTaskExecutor = asyncServiceTaskExecutor;
    }

    @Override
    public CompletableFuture<Void> runHarvestZipAsync(MultipartFile file, DatasetMetadata datasetMetadata){
        try {
            Record.RecordBuilder recordDataEncapsulated = Record.builder()
                                                                .datasetId(datasetMetadata.getDatasetId())
                                                                .datasetName(datasetMetadata.getDatasetName())
                                                                .country(datasetMetadata.getCountry())
                                                                .language(datasetMetadata.getLanguage());
            return runHarvestZipAsync(file.getInputStream(), recordDataEncapsulated, datasetMetadata);
        } catch (IOException e) {
            throw new ServiceException("Error harvesting records from file " + file.getName(), e);
        }
    }

    @Override
    public CompletableFuture<Void> runHarvestHttpZipAsync(String url, DatasetMetadata datasetMetadata){
        try {
            InputStream input = new URL(url).openStream();
            Record.RecordBuilder recordDataEncapsulated = Record.builder()
                                                                .datasetId(datasetMetadata.getDatasetId())
                                                                .datasetName(datasetMetadata.getDatasetName())
                                                                .country(datasetMetadata.getCountry())
                                                                .language(datasetMetadata.getLanguage());


            return runHarvestZipAsync(input, recordDataEncapsulated, datasetMetadata);
        } catch (IOException e) {
            throw new ServiceException("Error harvesting records from file " + url, e);
        }
    }

    private CompletableFuture<Void> runHarvestZipAsync(InputStream inputStreamToHarvest,
                                                       Record.RecordBuilder recordDataEncapsulated,
                                                       DatasetMetadata datasetMetadata) {
        return CompletableFuture.runAsync(() -> {
            try {
                harvestService.harvest(inputStreamToHarvest, datasetMetadata.getDatasetId(), recordDataEncapsulated,
                        datasetMetadata.getStepSize());
            } catch (HarvesterException e) {
                throw new ServiceException("Error harvesting records for dataset" + datasetMetadata.getDatasetId(), e);
            }
        }, asyncServiceTaskExecutor).thenRunAsync(() -> {
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
        return CompletableFuture.runAsync(
                () -> harvestService.harvestOaiPmh(datasetMetadata.getDatasetId(), recordDataEncapsulated, oaiHarvestData,
                        datasetMetadata.getStepSize()), asyncServiceTaskExecutor);
    }
}
