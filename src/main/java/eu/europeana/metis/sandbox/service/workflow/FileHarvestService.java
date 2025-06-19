package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.FileType;
import eu.europeana.metis.sandbox.entity.harvest.BinaryHarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import eu.europeana.metis.sandbox.service.util.HarvestServiceImpl;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.utils.CompressedFileExtension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FileHarvestService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final HarvestServiceImpl harvestServiceImpl;
  private final HarvestParameterService harvestParameterService;

  public FileHarvestService(HarvestServiceImpl harvestServiceImpl, HarvestParameterService harvestParameterService) {
    this.harvestServiceImpl = harvestServiceImpl;
    this.harvestParameterService = harvestParameterService;
  }

  public Map<String, HarvestedRecord> harvestRecordsFromFile(UUID harvestParameterId, String datasetId, int stepSize)
      throws IOException, EuropeanaIdException {
    HarvestParametersEntity harvestParametersEntity =
        harvestParameterService.getHarvestingParametersById(harvestParameterId).orElseThrow();

    String fileName;
    FileType fileType;
    byte[] fileContent;

    if (harvestParametersEntity instanceof BinaryHarvestParametersEntity binaryHarvestParametersEntity) {
      fileName = binaryHarvestParametersEntity.getFileName();
      fileType = binaryHarvestParametersEntity.getFileType();
      fileContent = binaryHarvestParametersEntity.getFileContent();
    } else {
      throw new IllegalArgumentException("Unsupported HarvestParametersEntity type for FileHarvest");
    }

    InputStream inputStream = new ByteArrayInputStream(fileContent);
    Map<String, String> recordIdAndContent = new HashMap<>();

    if (fileType.equals(FileType.XML)) {
      recordIdAndContent.put(fileName, IOUtils.toString(inputStream, StandardCharsets.UTF_8));
    } else {
      recordIdAndContent.putAll(harvestServiceImpl.harvestFromCompressedArchive(
          inputStream, datasetId, stepSize, CompressedFileExtension.valueOf(fileType.name())));
    }

    Map<String, HarvestedRecord> harvestedRecords = new HashMap<>();
    for (Map.Entry<String, String> entry : recordIdAndContent.entrySet()) {
      String sourceRecordId = entry.getKey();
      String recordData = entry.getValue();

      EuropeanaIdCreator europeanaIdCreator = new EuropeanaIdCreator();
      String sourceProvidedChoAbout = sourceRecordId;
      String recordId = sourceRecordId;
      try {
        EuropeanaGeneratedIdsMap idsMap = europeanaIdCreator.constructEuropeanaId(recordData, datasetId);
        sourceProvidedChoAbout = idsMap.getSourceProvidedChoAbout();
        recordId = idsMap.getEuropeanaGeneratedId();
      } catch (EuropeanaIdException e) {
        LOGGER.debug("Reading edm ids failed(probably not edm format), proceed without them", e);
      }

      harvestedRecords.put(sourceRecordId, new HarvestedRecord(sourceProvidedChoAbout, recordId, recordData));
    }

    return harvestedRecords;
  }
}

