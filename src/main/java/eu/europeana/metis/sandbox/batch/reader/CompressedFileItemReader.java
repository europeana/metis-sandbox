package eu.europeana.metis.sandbox.batch.reader;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.FILE_HARVEST;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.service.workflow.HarvestServiceImpl;
import eu.europeana.metis.utils.CompressedFileExtension;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class CompressedFileItemReader implements ItemReader<ExecutionRecordDTO> {

  private static final BatchJobType batchJobType = FILE_HARVEST;

  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;
  @Value("#{jobParameters['inputFilePath']}")
  private String inputFilePath;
  @Value("#{jobParameters['compressedFileExtension']}")
  private String compressedFileExtension;
  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  @Value("#{jobParameters['stepSize']}")
  private String stepSize;

  private final HarvestServiceImpl harvestServiceImpl;
  private Iterator<Entry<String, String>> recordIdAndContentIterator;

  public CompressedFileItemReader(HarvestServiceImpl harvestServiceImpl) {
    this.harvestServiceImpl = harvestServiceImpl;
  }

  @PostConstruct
  private void prepare() throws IOException {
    InputStream inputStream = new FileInputStream(inputFilePath);
    Map<String, String> recordIdAndContent = harvestServiceImpl.harvestFromCompressedArchive(inputStream, datasetId,
        Integer.valueOf(stepSize), CompressedFileExtension.valueOf(compressedFileExtension));
    recordIdAndContentIterator = recordIdAndContent.entrySet().iterator();
  }

  @Override
  public ExecutionRecordDTO read() {
    Entry<String, String> recordIdAndContent = takeRecordIdAndContent();
    if (recordIdAndContent != null) {
      SuccessExecutionRecordDTO successExecutionRecordDTO = SuccessExecutionRecordDTO.builder()
                                                                                     .datasetId(datasetId)
                                                                                     .recordId(recordIdAndContent.getKey())
                                                                                     .executionId(targetExecutionId)
                                                                                     .executionName(batchJobType.name())
                                                                                     .recordData(recordIdAndContent.getValue())
                                                                                     .build();
      return successExecutionRecordDTO;
    } else {
      return null;
    }
  }

  private synchronized Map.Entry<String, String> takeRecordIdAndContent() {
    if (recordIdAndContentIterator.hasNext()) {
      Map.Entry<String, String> entry = recordIdAndContentIterator.next();
      recordIdAndContentIterator.remove(); // Removes from the map
      return entry;
    } else {
      return null;
    }
  }

}
