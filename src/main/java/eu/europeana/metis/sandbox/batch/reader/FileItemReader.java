package eu.europeana.metis.sandbox.batch.reader;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.HARVEST_FILE;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.common.HarvestedRecord;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionSetupService;
import eu.europeana.metis.sandbox.service.workflow.FileHarvestService;
import eu.europeana.metis.sandbox.service.workflow.FileHarvestService.FileHarvestException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A Spring Batch {@link ItemReader} implementation that reads records from a file using the {@link FileHarvestService}.
 *
 * <p>It retrieves job parameters such as target execution ID, harvest parameter ID, dataset ID,
 * and step size to determine the reading context.
 *
 * <p>The reader initializes an iterator OF harvested records using {@link FileHarvestService}
 * during the {@code @PostConstruct} phase to prepare the data for batch processing.
 *
 * <p>Each call to {@link #read()} returns an {@link AbstractExecutionRecordDTO}, representing a single
 * successfully validated record, or {@code null} when no further records are available.
 *
 * <p>Note: This reader is not restartable and is not optimized at its current state.
 */
@StepScope
@Component
public class FileItemReader implements ItemReader<AbstractExecutionRecordDTO> {

  private static final BatchJobType batchJobType = HARVEST_FILE;

  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;
  @Value("#{jobParameters['harvestParameterId']}")
  private String harvestParameterId;
  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  @Value("#{jobParameters['stepSize']}")
  private String stepSize;

  private final FileHarvestService fileHarvestService;
  private final DatasetExecutionSetupService datasetExecutionSetupService;
  private Iterator<Entry<String, HarvestedRecord>> recordIdAndContentIterator;

  /**
   * Constructor with service parameter.
   *
   * @param fileHarvestService FileHarvestService instance used for reading and processing files.
   * @param datasetExecutionSetupService DatasetExecutionSetupService instance used for updating dataset errors.
   */
  public FileItemReader(FileHarvestService fileHarvestService, DatasetExecutionSetupService datasetExecutionSetupService) {
    this.fileHarvestService = fileHarvestService;
    this.datasetExecutionSetupService = datasetExecutionSetupService;
  }

  /**
   * Prepares for the execution of a single step by harvesting records from a file and initializing the iterator for processing
   * harvested records.
   * <p>
   * If an exception occurs during the harvesting process, updates the dataset with the associated error.
   *
   * @param stepExecution the context of the currently executing step in the batch process
   * @throws FileHarvestException if an error occurs during the file harvesting operation
   */
  @BeforeStep
  public void beforeStep(StepExecution stepExecution) throws FileHarvestException {
    try {
      Map<String, HarvestedRecord> recordIdAndContent = fileHarvestService.harvestRecordsFromFile(
          UUID.fromString(harvestParameterId),
          datasetId, Integer.parseInt(stepSize));
      recordIdAndContentIterator = recordIdAndContent.entrySet().iterator();
    } catch (RuntimeException | FileHarvestException exception) {
      datasetExecutionSetupService.updateDatasetWithError(datasetId, exception);
      throw exception;
    }
  }

  @Override
  public AbstractExecutionRecordDTO read() {
    try {
      Entry<String, HarvestedRecord> recordIdAndContent = takeRecordIdAndContent();
      if (recordIdAndContent == null) {
        return null;
      } else {
        return SuccessExecutionRecordDTO.createValidated(b -> b
            .datasetId(datasetId)
            .sourceRecordId(recordIdAndContent.getValue().sourceRecordId())
            .recordId(recordIdAndContent.getValue().recordId())
            .executionId(targetExecutionId)
            .executionName(batchJobType.name())
            .recordData(recordIdAndContent.getValue().recordData()));
      }
    } catch (RuntimeException exception) {
      datasetExecutionSetupService.updateDatasetWithError(datasetId, exception);
      throw exception;
    }
  }

  private synchronized Entry<String, HarvestedRecord> takeRecordIdAndContent() {
    if (recordIdAndContentIterator.hasNext()) {
      Entry<String, HarvestedRecord> entry = recordIdAndContentIterator.next();
      recordIdAndContentIterator.remove();
      return entry;
    } else {
      return null;
    }
  }
}
