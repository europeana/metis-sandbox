package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.config.BatchJobType.OAI_HARVEST;

import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.sandbox.batch.config.BatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class OaiRecordHarvesterItemProcessor implements ItemProcessor<ExecutionRecordExternalIdentifier, ExecutionRecordDTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final BatchJobType batchJobType = OAI_HARVEST;

    final OaiHarvester oaiHarvester = HarvesterFactory.createOaiHarvester();

    @Value("#{jobParameters['oaiEndpoint']}")
    private String oaiEndpoint;
    @Value("#{jobParameters['oaiSet']}")
    private String oaiSet;
    @Value("#{jobParameters['oaiMetadataPrefix']}")
    private String oaiMetadataPrefix;
    @Value("#{jobParameters['datasetId']}")
    private String datasetId;
    @Value("#{jobParameters['overrideJobId'] ?: stepExecution.jobExecution.jobInstance.id}")
    private Long jobInstanceId;

    @Override
    public ExecutionRecordDTO process(ExecutionRecordExternalIdentifier executionRecordExternalIdentifier) throws Exception {

        LOGGER.info("OaiHarvestItemReader thread: {}", Thread.currentThread());
        OaiHarvest oaiHarvest = new OaiHarvest(oaiEndpoint, oaiMetadataPrefix, oaiSet);
        final OaiRecord oaiRecord = oaiHarvester.harvestRecord(oaiHarvest, executionRecordExternalIdentifier.getIdentifier().getRecordId());
        String resultString = new String(oaiRecord.getContent().readAllBytes(), StandardCharsets.UTF_8);
        return getExecutionRecordDTO(resultString);
    }

    private ExecutionRecordDTO getExecutionRecordDTO(String resultString) throws EuropeanaIdException {
        EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
        final EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = europeanIdCreator.constructEuropeanaId(resultString, datasetId);
        final String europeanaGeneratedId = europeanaGeneratedIdsMap.getEuropeanaGeneratedId();
        final ExecutionRecordDTO executionRecordDTO = new ExecutionRecordDTO();
        executionRecordDTO.setDatasetId(datasetId);
        executionRecordDTO.setExecutionId(jobInstanceId.toString());
        executionRecordDTO.setRecordId(europeanaGeneratedId);
        executionRecordDTO.setExecutionName(batchJobType.name());
        executionRecordDTO.setRecordData(resultString);
        return executionRecordDTO;
    }
}
