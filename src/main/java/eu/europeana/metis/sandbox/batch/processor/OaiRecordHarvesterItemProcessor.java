package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createValidated;

import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@Component
@StepScope
public class OaiRecordHarvesterItemProcessor extends
    AbstractMetisItemProcessor<ExecutionRecordExternalIdentifier, ExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  final OaiHarvester oaiHarvester = HarvesterFactory.createOaiHarvester();

  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;
  @Value("#{jobParameters['oaiEndpoint']}")
  private String oaiEndpoint;
  @Value("#{jobParameters['oaiSet']}")
  private String oaiSet;
  @Value("#{jobParameters['oaiMetadataPrefix']}")
  private String oaiMetadataPrefix;
  @Value("#{jobParameters['datasetId']}")
  private String datasetId;

  @Override
  public ExecutionRecordDTO process(ExecutionRecordExternalIdentifier executionRecordExternalIdentifier) throws Exception {

    LOGGER.info("OaiHarvestItemReader thread: {}", Thread.currentThread());
    OaiHarvest oaiHarvest = new OaiHarvest(oaiEndpoint, oaiMetadataPrefix, oaiSet);
    final OaiRecord oaiRecord = oaiHarvester.harvestRecord(oaiHarvest,
        executionRecordExternalIdentifier.getIdentifier().getRecordId());
    String resultString = new String(oaiRecord.getContent().readAllBytes(), StandardCharsets.UTF_8);
    return getExecutionRecordDTO(resultString);
  }

  private ExecutionRecordDTO getExecutionRecordDTO(String resultString) throws EuropeanaIdException {
    EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
    final EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = europeanIdCreator.constructEuropeanaId(resultString, datasetId);
    final String europeanaGeneratedId = europeanaGeneratedIdsMap.getEuropeanaGeneratedId();
    return createValidated(
        b -> b.datasetId(datasetId)
              .executionId(targetExecutionId)
              .recordId(europeanaGeneratedId)
              .executionName(getExecutionName())
              .recordData(resultString));
  }

  @Override
  public ThrowingFunction<SuccessExecutionRecordDTO, SuccessExecutionRecordDTO> getProcessRecordFunction() {
    return null;
  }
}
