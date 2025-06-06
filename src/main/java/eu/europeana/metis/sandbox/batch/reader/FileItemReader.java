package eu.europeana.metis.sandbox.batch.reader;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.HARVEST_FILE;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.service.workflow.FileHarvestService;
import eu.europeana.metis.sandbox.service.workflow.HarvestedRecord;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@StepScope
@Component
public class FileItemReader implements ItemReader<ExecutionRecordDTO> {

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
  private Iterator<Entry<String, HarvestedRecord>> recordIdAndContentIterator;

  public FileItemReader(FileHarvestService fileHarvestService) {
    this.fileHarvestService = fileHarvestService;
  }

  @PostConstruct
  private void prepare() throws IOException, EuropeanaIdException {
    Map<String, HarvestedRecord> recordIdAndContent = fileHarvestService.harvestRecordsFromFile(UUID.fromString(harvestParameterId),
        datasetId, Integer.parseInt(stepSize));
    recordIdAndContentIterator = recordIdAndContent.entrySet().iterator();
  }

  @Override
  public ExecutionRecordDTO read() {
    Entry<String, HarvestedRecord> recordIdAndContent = takeRecordIdAndContent();
    if (recordIdAndContent != null) {
      return SuccessExecutionRecordDTO.createValidated(b -> b
          .datasetId(datasetId)
          .sourceRecordId(recordIdAndContent.getValue().sourceRecordId())
          .recordId(recordIdAndContent.getValue().recordId())
          .executionId(targetExecutionId)
          .executionName(batchJobType.name())
          .recordData(recordIdAndContent.getValue().recordData()));
    } else {
      return null;
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
