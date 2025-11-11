package eu.europeana.metis.sandbox.batch.reader;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.HARVEST_FILE;

import eu.europeana.metis.harvesting.HarvestingIterator;
import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRunRepository;
import eu.europeana.metis.sandbox.entity.harvest.AbstractBinaryHarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionSetupService;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import eu.europeana.metis.sandbox.service.workflow.harvest.FileHarvestService;
import eu.europeana.metis.sandbox.service.workflow.harvest.FileHarvestTarget;
import java.nio.file.Path;
import java.util.function.Function;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AbstractIdentifiersItemReader} specialized for reading and processing external file identifiers. This
 * class is designed to harvest identifiers from file-based sources and process them in a step-oriented batch job.
 */
@Component
@StepScope
public class FileIdentifiersItemReader extends AbstractIdentifiersItemReader<Path> {

  private static final BatchJobType batchJobType = HARVEST_FILE;
  private final FileHarvestService fileHarvestService;

  /**
   * Constructor.
   *
   * @param harvestParameterService the service responsible for managing harvest parameters related to datasets
   * @param datasetExecutionSetupService the service responsible for the setup and management of dataset execution
   * @param fileHarvestService the service responsible for harvesting identifiers from file-based sources
   * @param executionRunRepository the repository used to store execution runs
   */
  public FileIdentifiersItemReader(HarvestParameterService harvestParameterService,
      DatasetExecutionSetupService datasetExecutionSetupService, FileHarvestService fileHarvestService,
      ExecutionRunRepository executionRunRepository) {
    super(harvestParameterService, datasetExecutionSetupService, executionRunRepository);
    this.fileHarvestService = fileHarvestService;
  }

  @Override
  protected Function<Path, Path> getIdentifierTransformer() {
    return path -> FileHarvestService.getDeterministicPathToTempDirectoryById(datasetId).relativize(path);
  }

  @Override
  protected HarvestingIterator<Path, Path> getHarvestingIterator(HarvestParametersEntity params, int stepSize) {
    if (!(params instanceof AbstractBinaryHarvestParametersEntity abstractBinaryHarvestParametersEntity)) {
      throw new IllegalArgumentException("Expected AbstractBinaryHarvestParametersEntity");
    }
    FileHarvestTarget fileHarvestTarget = new FileHarvestTarget(
        abstractBinaryHarvestParametersEntity.getFileName(),
        abstractBinaryHarvestParametersEntity.getFileType(),
        abstractBinaryHarvestParametersEntity.getFileContent());
    return fileHarvestService.getHarvestingIteratorIdentifiers(datasetId, fileHarvestTarget);
  }

  @Override
  protected String extractStringIdentifier(Path identifier) {
    return identifier.toString();
  }

  @Override
  protected boolean isDeleted(Path identifier) {
    return false;
  }

  @Override
  protected BatchJobType getJobType() {
    return batchJobType;
  }
}
