package eu.europeana.metis.sandbox.batch.reader;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.HARVEST_OAI;

import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRepository;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.OaiHarvestParametersEntity;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionSetupService;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import eu.europeana.metis.sandbox.service.workflow.harvest.HarvestIdentifiersResult;
import eu.europeana.metis.sandbox.service.workflow.harvest.OaiHarvestService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AbstractIdentifiersItemReader} specialized for reading and processing external oaipmh identifiers.
 * This class is designed to harvest identifiers from oaipmh sources and process them in a step-oriented batch job.
 */
@StepScope
@Component
public class OaiIdentifiersItemReader extends AbstractIdentifiersItemReader<OaiRecordHeader> {

  private static final BatchJobType batchJobType = HARVEST_OAI;
  private final OaiHarvestService oaiHarvestService;

  /**
   * Constructor.
   *
   * @param harvestParameterService the service responsible for managing harvest parameters related to datasets
   * @param datasetExecutionSetupService the service responsible for the setup and management of dataset execution
   * @param oaiHarvestService the service responsible for harvesting OAI identifiers from the specified endpoint
   */
  public OaiIdentifiersItemReader(HarvestParameterService harvestParameterService,
      DatasetExecutionSetupService datasetExecutionSetupService, OaiHarvestService oaiHarvestService,
      ExecutionRepository executionRepository) {
    super(harvestParameterService, datasetExecutionSetupService, executionRepository);
    this.oaiHarvestService = oaiHarvestService;
  }

  @Override
  protected HarvestIdentifiersResult<OaiRecordHeader> doHarvest(HarvestParametersEntity harvestParametersEntity, int stepSize) {
    if (!(harvestParametersEntity instanceof OaiHarvestParametersEntity oaiHarvestParametersEntity)) {
      throw new IllegalArgumentException("Expected OaiHarvestParametersEntity");
    }
    OaiHarvest oaiHarvest = new OaiHarvest(
        oaiHarvestParametersEntity.getUrl(),
        oaiHarvestParametersEntity.getMetadataFormat(),
        oaiHarvestParametersEntity.getSetSpec());
    return oaiHarvestService.harvestExternalIdentifiers(oaiHarvest, stepSize);
  }

  @Override
  protected String extractStringIdentifier(OaiRecordHeader identifier) {
    return identifier.getOaiIdentifier();
  }

  @Override
  protected boolean isDeleted(OaiRecordHeader identifier) {
    return identifier.isDeleted();
  }

  @Override
  protected BatchJobType getJobType() {
    return batchJobType;
  }
}
