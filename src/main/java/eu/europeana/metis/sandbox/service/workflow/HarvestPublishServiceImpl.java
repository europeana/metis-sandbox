package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
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
    public CompletableFuture<Void> runHarvestZipAsync(MultipartFile file, String datasetName, String datasetId, Country country, Language language){
        try {
            Record.RecordBuilder recordDataEncapsulated = Record.builder()
                                                                .datasetId(datasetId)
                                                                .datasetName(datasetName)
                                                                .country(country)
                                                                .language(language);
            return runHarvestZipAsync(file.getInputStream(), datasetId, recordDataEncapsulated);
        } catch (IOException e) {
            throw new ServiceException("Error harvesting records from file " + file.getName(), e);
        }
    }

    @Override
    public CompletableFuture<Void> runHarvestHttpZipAsync(String url, String datasetName, String datasetId, Country country, Language language){
        try {
            InputStream input = new URL(url).openStream();
            Record.RecordBuilder recordDataEncapsulated = Record.builder()
                                                                .datasetId(datasetId)
                                                                .datasetName(datasetName)
                                                                .country(country)
                                                                .language(language);
            return runHarvestZipAsync(input, datasetId, recordDataEncapsulated);
        } catch (IOException e) {
            throw new ServiceException("Error harvesting records from file " + url, e);
        }
    }

    private CompletableFuture<Void> runHarvestZipAsync(InputStream inputStreamToHarvest, String datasetId, Record.RecordBuilder recordDataEncapsulated) {
        return CompletableFuture.runAsync(() -> {
            try {
                harvestService.harvest(inputStreamToHarvest, datasetId, recordDataEncapsulated);
            } catch (HarvesterException e) {
                throw new ServiceException("Error harvesting records for dataset" + datasetId, e);
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
    public CompletableFuture<Void> runHarvestOaiPmhAsync(String datasetName, String datasetId,
                                                      Country country, Language language, OaiHarvestData oaiHarvestData) {
        Record.RecordBuilder recordDataEncapsulated = Record.builder().country(country).language(language).datasetName(datasetName).datasetId(datasetId);
        return CompletableFuture.runAsync(
                () -> harvestService.harvestOaiPmh(datasetId, recordDataEncapsulated, oaiHarvestData), asyncServiceTaskExecutor);
    }
}
