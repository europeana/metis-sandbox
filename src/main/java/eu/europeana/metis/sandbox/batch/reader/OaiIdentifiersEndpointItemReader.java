package eu.europeana.metis.sandbox.batch.reader;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.OAI_HARVEST;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.HarvestingIterator;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class OaiIdentifiersEndpointItemReader implements ItemReader<ExecutionRecordExternalIdentifier> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final BatchJobType batchJobType = OAI_HARVEST;

    @Value("#{jobParameters['targetExecutionId']}")
    private String targetExecutionId;
    @Value("#{jobParameters['datasetId']}")
    private String datasetId;
    @Value("#{jobParameters['oaiEndpoint']}")
    private String oaiEndpoint;
    @Value("#{jobParameters['oaiSet']}")
    private String oaiSet;
    @Value("#{jobParameters['oaiMetadataPrefix']}")
    private String oaiMetadataPrefix;

    final OaiHarvester oaiHarvester = HarvesterFactory.createOaiHarvester();
    private final List<OaiRecordHeader> oaiRecordHeaders = new LinkedList<>();

    @PostConstruct
    private void prepare() throws HarvesterException, IOException {
        harvestIdentifiers();
    }

    @Override
    public ExecutionRecordExternalIdentifier read() {

        final OaiRecordHeader oaiRecordHeader = takeIdentifier();
        if (oaiRecordHeader != null) {
            ExecutionRecordIdentifier executionRecordIdentifier = new ExecutionRecordIdentifier();
            executionRecordIdentifier.setDatasetId(datasetId);
            executionRecordIdentifier.setRecordId(oaiRecordHeader.getOaiIdentifier());
            executionRecordIdentifier.setExecutionId(targetExecutionId);
            executionRecordIdentifier.setExecutionName(batchJobType.name());

            ExecutionRecordExternalIdentifier recordIdentifier = new ExecutionRecordExternalIdentifier();
            recordIdentifier.setIdentifier(executionRecordIdentifier);
            recordIdentifier.setDeleted(oaiRecordHeader.isDeleted());

            return recordIdentifier;
        } else {
            return null;
        }
    }

    private void harvestIdentifiers() throws HarvesterException, IOException {
        LOGGER.info("Harvesting identifiers for {}", oaiEndpoint);
        OaiHarvest oaiHarvest = new OaiHarvest(oaiEndpoint, oaiMetadataPrefix, oaiSet);
        StopWatch watch = StopWatch.createStarted();
        try (HarvestingIterator<OaiRecordHeader, OaiRecordHeader> headerIterator =
            oaiHarvester.harvestRecordHeaders(oaiHarvest)) {
            headerIterator.forEach(oaiRecordHeader -> {
                oaiRecordHeaders.add(oaiRecordHeader);
                if (watch.getTime(TimeUnit.SECONDS) > 10) {
                    LOGGER.info("Already harvested {} records...", oaiRecordHeaders.size());
                    watch.reset();
                    watch.start();
                }
                return ReportingIteration.IterationResult.CONTINUE;
            });
        }
        LOGGER.info("Identifiers harvested");
    }

    private synchronized OaiRecordHeader takeIdentifier() {
        if (!oaiRecordHeaders.isEmpty()) {
            return oaiRecordHeaders.removeFirst();
        } else {
            return null;
        }
    }
}
