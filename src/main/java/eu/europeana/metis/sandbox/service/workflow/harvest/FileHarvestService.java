package eu.europeana.metis.sandbox.service.workflow.harvest;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.HarvestingIterator;
import eu.europeana.metis.harvesting.ReportingIteration.IterationResult;
import eu.europeana.metis.harvesting.file.FileHarvester;
import eu.europeana.metis.sandbox.common.FileType;
import eu.europeana.metis.sandbox.common.HarvestedRecord;
import eu.europeana.metis.sandbox.common.exception.HarvestException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.service.workflow.harvest.OaiHarvestService.HarvestFromIteratorResult;
import eu.europeana.metis.utils.TempFileUtils;
import jakarta.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for harvesting records from file-based inputs.
 *
 * <p>Supports both XML and compressed file formats.
 */
@Slf4j
@Service
public class FileHarvestService implements HarvestService<String, FileHarvestTarget> {

  private final FileHarvester fileHarvester = HarvesterFactory.createFileHarvester();
  private final int maxAllowedRecords;

  /**
   * Constructor.
   *
   * @param maxAllowedRecords the maximum number of records allowed to be processed
   */
  @Autowired
  public FileHarvestService(@Value("${sandbox.dataset.max-size}") int maxAllowedRecords) {
    this.maxAllowedRecords = maxAllowedRecords;
  }

  @Override
  public HarvestIdentifiersResult<String> harvestExternalIdentifiers(String datasetId,
      @NotNull FileHarvestTarget fileHarvestTarget,
      Integer stepSize) {
    if (fileHarvestTarget.fileType().equals(FileType.XML)) {
      return new HarvestIdentifiersResult<>(List.of(fileHarvestTarget.fileName()), false);
    } else {
      return harvestIdentifiersFromCompressedArchive(datasetId, fileHarvestTarget, stepSize);
    }
  }

  @Override
  public HarvestedRecord harvestRecord(String datasetId, @NotNull FileHarvestTarget fileHarvestTarget, String sourceRecordId)
      throws HarvestException {
    if (fileHarvestTarget.fileType().equals(FileType.XML)) {
      InputStream inputStream = new ByteArrayInputStream(fileHarvestTarget.fileContent());
      String recordData = getStringData(inputStream);
      return new HarvestedRecord(sourceRecordId, recordData);
    } else {
      return harvestRecordFromArchive(datasetId, sourceRecordId);
    }
  }

  @Override
  public int getMaxAllowedRecords() {
    return maxAllowedRecords;
  }

  private HarvestIdentifiersResult<String> harvestIdentifiersFromCompressedArchive(String datasetId,
      @NotNull FileHarvestTarget fileHarvestTarget, Integer stepSize) {
    Path destinationDirectory = getPathToTempDestinationDirectoryById(datasetId);
    Path destinationArchiveFile;
    try {
      TempFileUtils.createSecureDirectory(destinationDirectory);
      destinationArchiveFile = destinationDirectory.resolve(Path.of(fileHarvestTarget.fileName()));
      Files.write(destinationArchiveFile, fileHarvestTarget.fileContent());
    } catch (IOException e) {
      throw new ServiceException("Error creating temporary file", e);
    }

    try {
      //Do not close because the directory is then deleted.
      HarvestingIterator<Path, Path> pathIterator =
          fileHarvester.createHarvestIterator(destinationArchiveFile, destinationArchiveFile.getParent());
      final List<String> result = new ArrayList<>();
      HarvestFromIteratorResult harvestFromIteratorResult = harvestFromIterator(pathIterator, stepSize, entry -> {
        Path relativePath = destinationDirectory.relativize(entry);
        result.add(relativePath.toString());
        return IterationResult.CONTINUE;
      }, (p) -> false);
      return new HarvestIdentifiersResult<>(result, harvestFromIteratorResult.recordLimitExceeded());
    } catch (HarvesterException e) {
      throw new ServiceException("Error harvesting File records", e);
    }
  }

  public static @NotNull Path getPathToTempDestinationDirectoryById(String id) {
    return Path.of(System.getProperty("java.io.tmpdir"), FileHarvestService.class.getSimpleName() + "-" + id);
  }

  private HarvestedRecord harvestRecordFromArchive(String datasetId, String sourceRecordId) throws HarvestException {
    String recordData = getRecord(datasetId, sourceRecordId);
    return new HarvestedRecord(sourceRecordId, recordData);
  }

  private String getRecord(String datasetId, String sourceRecordId) throws HarvestException {
    try {
      Path destinationDirectory = getPathToTempDestinationDirectoryById(datasetId);
      Path recordFilePath = destinationDirectory.resolve(Path.of(sourceRecordId));
      return Files.readString(recordFilePath);
    } catch (IOException e) {
      throw new HarvestException(e);
    }
  }

  private String getStringData(InputStream inputStream) throws HarvestException {
    try {
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new HarvestException(e);
    }
  }
}

