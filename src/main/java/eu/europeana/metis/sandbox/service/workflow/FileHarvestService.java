package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Optional.ofNullable;

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
import java.util.Optional;
import java.util.UUID;
import lombok.experimental.StandardException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for harvesting records from file-based inputs.
 *
 * <p>Supports both XML and compressed file formats.
 */
@Service
public class FileHarvestService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final HarvestServiceImpl harvestServiceImpl;
  private final HarvestParameterService harvestParameterService;

  /**
   * Constructor.
   *
   * @param harvestServiceImpl provides implementation of harvest processing logic
   * @param harvestParameterService facilitates access to harvest parameters from the dataset
   */
  public FileHarvestService(HarvestServiceImpl harvestServiceImpl, HarvestParameterService harvestParameterService) {
    this.harvestServiceImpl = harvestServiceImpl;
    this.harvestParameterService = harvestParameterService;
  }

  /**
   * Harvests records from a file based on the specified harvest parameters, dataset identifier, and step size.
   *
   * <p>Supports XML and compressed file formats.
   * <p>Processes records to generate a map of source record IDs to harvested records.
   *
   * @param harvestParameterId the unique identifier for harvest parameters
   * @param datasetId the identifier for the dataset being harvested
   * @param stepSize the step size
   * @return a map where keys are source record IDs and values are harvested records
   * @throws FileHarvestException if an error occurs
   */
  public Map<String, HarvestedRecord> harvestRecordsFromFile(UUID harvestParameterId, String datasetId, int stepSize)
      throws FileHarvestException {
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
      String stringData = getStringData(inputStream);
      recordIdAndContent.put(fileName, stringData);
    } else {
      recordIdAndContent.putAll(harvestServiceImpl.harvestFromCompressedArchive(inputStream, stepSize,
          CompressedFileExtension.valueOf(fileType.name())));
    }

    Map<String, HarvestedRecord> harvestedRecords = new HashMap<>();
    for (Map.Entry<String, String> entry : recordIdAndContent.entrySet()) {
      String sourceRecordId = entry.getKey();
      String recordData = entry.getValue();

      Optional<EuropeanaGeneratedIdsMap> europeanaGeneratedIdsMap = getEuropeanaGeneratedIdsMap(datasetId, recordData);
      String sourceProvidedChoAbout = europeanaGeneratedIdsMap.map(EuropeanaGeneratedIdsMap::getSourceProvidedChoAbout)
                                                              .orElse(sourceRecordId);
      String recordId = europeanaGeneratedIdsMap.map(EuropeanaGeneratedIdsMap::getEuropeanaGeneratedId).orElse(sourceRecordId);
      harvestedRecords.put(sourceRecordId, new HarvestedRecord(sourceProvidedChoAbout, recordId, recordData));
    }

    return harvestedRecords;
  }

  private String getStringData(InputStream inputStream) throws FileHarvestException {
    try {
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new FileHarvestException(e);
    }
  }

  private Optional<EuropeanaGeneratedIdsMap> getEuropeanaGeneratedIdsMap(String datasetId, String recordData) {
    EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = null;
    try {
      EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
      europeanaGeneratedIdsMap = europeanIdCreator.constructEuropeanaId(recordData, datasetId);
    } catch (EuropeanaIdException e) {
      LOGGER.debug("Reading edm ids failed(probably not edm format), proceed without them", e);
    }
    return ofNullable(europeanaGeneratedIdsMap);
  }

  /**
   * Exception thrown during file harvest operations when an error occurs.
   */
  @StandardException
  public static class FileHarvestException extends Exception {

  }
}

