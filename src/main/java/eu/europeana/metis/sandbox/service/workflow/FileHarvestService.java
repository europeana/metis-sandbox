package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.FileType;
import eu.europeana.metis.sandbox.entity.harvest.BinaryHarvestParameters;
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

@Service
public class FileHarvestService {

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

    if (harvestParametersEntity instanceof BinaryHarvestParameters binaryHarvestParameters) {
      fileName = binaryHarvestParameters.getFileName();
      fileType = binaryHarvestParameters.getFileType();
      fileContent = binaryHarvestParameters.getFileContent();
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

    // Map to HarvestedRecord
    Map<String, HarvestedRecord> harvestedRecords = new HashMap<>();

    for (Map.Entry<String, String> entry : recordIdAndContent.entrySet()) {
      String sourceRecordId = entry.getKey();
      String recordData = entry.getValue();

      EuropeanaIdCreator europeanaIdCreator = new EuropeanaIdCreator();
      EuropeanaGeneratedIdsMap idsMap = europeanaIdCreator.constructEuropeanaId(recordData, datasetId);

      harvestedRecords.put(sourceRecordId, new HarvestedRecord(
          idsMap.getSourceProvidedChoAbout(),
          idsMap.getEuropeanaGeneratedId(),
          recordData
      ));
    }

    return harvestedRecords;
  }
}

