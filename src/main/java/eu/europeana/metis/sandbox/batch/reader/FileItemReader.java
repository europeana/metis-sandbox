package eu.europeana.metis.sandbox.batch.reader;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.HARVEST_FILE;
import static org.apache.tika.utils.StringUtils.isBlank;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.service.workflow.HarvestServiceImpl;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.utils.CompressedFileExtension;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.IOUtils;
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

  public FileItemReader(HarvestServiceImpl harvestServiceImpl) {
    this.harvestServiceImpl = harvestServiceImpl;
  }

  @PostConstruct
  private void prepare() throws IOException {
    InputStream inputStream = new FileInputStream(inputFilePath);
    String recordId = Paths.get(inputFilePath).getFileName().toString();

    final Map<String, String> recordIdAndContent = new HashMap<>();
    if (isBlank(compressedFileExtension)) {
      recordIdAndContent.put(recordId, IOUtils.toString(inputStream, StandardCharsets.UTF_8));
    } else {
      recordIdAndContent.putAll(harvestServiceImpl.harvestFromCompressedArchive(inputStream, datasetId,
          Integer.valueOf(stepSize), CompressedFileExtension.valueOf(compressedFileExtension)));
    }
    recordIdAndContentIterator = recordIdAndContent.entrySet().iterator();
  }

  @Override
  public ExecutionRecordDTO read() throws EuropeanaIdException {
    Entry<String, String> recordIdAndContent = takeRecordIdAndContent();
    if (recordIdAndContent != null) {
      EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
      final EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = europeanIdCreator.constructEuropeanaId(recordIdAndContent.getValue(), datasetId);
      return SuccessExecutionRecordDTO.createValidated(b -> b
          .datasetId(datasetId)
          .sourceRecordId(europeanaGeneratedIdsMap.getSourceProvidedChoAbout())
          .recordId(europeanaGeneratedIdsMap.getEuropeanaGeneratedId())
          .executionId(targetExecutionId)
          .executionName(batchJobType.name())
          .recordData(recordIdAndContent.getValue()));
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
