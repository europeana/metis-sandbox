package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.http.HttpRecordIterator;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AsyncHarvestPublishServiceImpl implements AsyncHarvestPublishService {

    private final HttpHarvester httpHarvester;
    private final HarvestService harvestService;
    private final Executor asyncServiceTaskExecutor;

    public AsyncHarvestPublishServiceImpl(HttpHarvester httpHarvester,
                                          HarvestService harvestService,
                                          Executor asyncServiceTaskExecutor) {
        this.httpHarvester = httpHarvester;
        this.harvestService = harvestService;
        this.asyncServiceTaskExecutor = asyncServiceTaskExecutor;

    }

    public CompletableFuture<Void> runHarvestAsync(InputStream inputStreamToHarvest) {
        return CompletableFuture.runAsync(() -> harvest(inputStreamToHarvest));
    }

    private void harvest(InputStream inputStream) {

        AtomicBoolean hasReachedRecordLimit = new AtomicBoolean(false);
        AtomicInteger numberOfIterations = new AtomicInteger(0);

        try {
            final HttpRecordIterator iterator = httpHarvester.createHttpHarvestIterator(inputStream, CompressedFileExtension.ZIP);
        } catch (HarvesterException e) {
            throw new ServiceException("Error harvesting records ", e);
        }

    }


    @Override
    public CompletableFuture<Void> runHarvestOaiAsync(String datasetName, String datasetId,
                                                      Country country, Language language, OaiHarvestData oaiHarvestData) {
        return CompletableFuture.runAsync(
                () -> harvestService.harvestOaiPmh(datasetName, datasetId, country, language, oaiHarvestData), asyncServiceTaskExecutor);

    }


}
