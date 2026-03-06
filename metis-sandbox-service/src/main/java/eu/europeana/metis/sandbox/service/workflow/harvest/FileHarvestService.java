package eu.europeana.metis.sandbox.service.workflow.harvest;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.HarvestingIterator;
import eu.europeana.metis.harvesting.file.FileHarvester;
import eu.europeana.metis.harvesting.file.PathIterator;
import eu.europeana.metis.sandbox.common.FileType;
import eu.europeana.metis.sandbox.common.HarvestedRecord;
import eu.europeana.metis.sandbox.common.exception.HarvestException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.utils.TempFileUtils;
import jakarta.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

/**
 * Service for harvesting records from file-based inputs.
 *
 * <p>Supports both XML and compressed file formats.
 */
@Slf4j
@Service
public class FileHarvestService implements HarvestService<Path, FileHarvestTarget> {

  private final FileHarvester fileHarvester = HarvesterFactory.createFileHarvester();

  @Override
  public HarvestingIterator<Path, Path> getHarvestingIteratorIdentifiers(String datasetId,
      @NotNull FileHarvestTarget fileHarvestTarget) {
    if (fileHarvestTarget.fileType().equals(FileType.XML)) {
      return getIterableHarvestingIdentifiersSingleFile(datasetId, fileHarvestTarget);
    } else {
      return getIterableHarvestingIdentifiersForArchive(datasetId, fileHarvestTarget);
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

  private HarvestingIterator<Path, Path> getIterableHarvestingIdentifiersSingleFile(String datasetId, @NotNull FileHarvestTarget fileHarvestTarget) {
    Path destinationArchiveFile = createFileInTempDirectoryAndGetPath(datasetId, fileHarvestTarget);
    return new PathIterator(destinationArchiveFile.getParent());
  }

  private HarvestingIterator<Path, Path> getIterableHarvestingIdentifiersForArchive(String datasetId, @NotNull FileHarvestTarget fileHarvestTarget) {
    Path destinationArchiveFile = createFileInTempDirectoryAndGetPath(datasetId, fileHarvestTarget);

    try {
      //Do not close because the directory is then deleted. Handle deletion elsewhere.
      return fileHarvester.createHarvestIteratorFromArchive(destinationArchiveFile, destinationArchiveFile.getParent());
    } catch (HarvesterException e) {
      throw new ServiceException("Error harvesting File records", e);
    }
  }

  private Path createFileInTempDirectoryAndGetPath(String datasetId, @NotNull FileHarvestTarget fileHarvestTarget) {
    Path destinationDirectory = getDeterministicPathToTempDirectoryById(datasetId);
    Path destinationArchiveFile;
    try {
      TempFileUtils.createSecureDirectory(destinationDirectory);
      destinationArchiveFile = destinationDirectory.resolve(Path.of(fileHarvestTarget.fileName()));
      Files.write(destinationArchiveFile, fileHarvestTarget.fileContent());
      return destinationArchiveFile;
    } catch (IOException e) {
      throw new ServiceException("Error creating temporary file", e);
    }
  }

  /**
   * Returns the path to a temporary destination directory for a given identifier.
   * The directory path is constructed within the system's temporary directory
   * and is uniquely tied to the provided identifier.
   *
   * @param id the unique identifier used to create the temporary destination directory path
   * @return the {@link Path} object representing the temporary destination directory
   */
  public static @NotNull Path getDeterministicPathToTempDirectoryById(String id) {
    return TempFileUtils.getDeterministicPathToTempDirectoryById(FileHarvestService.class.getSimpleName() + "-" + id);
  }

  /**
   * Converts a partitioned {@link Path} into its canonical string-based representation.
   *
   * @param partitionedPath the partitioned {@link Path} to be converted
   * @return the canonical string representation of the partitioned path
   */
  public @NotNull String convertPartitionedPathToCanonicalString(Path partitionedPath) {
    return fileHarvester.convertPartitionedPathToCanonical(partitionedPath).toString();
  }

  private HarvestedRecord harvestRecordFromArchive(String datasetId, String sourceRecordId) throws HarvestException {
    String recordData = getRecord(datasetId, sourceRecordId);
    return new HarvestedRecord(sourceRecordId, recordData);
  }

  private String getRecord(String datasetId, String sourceRecordId) throws HarvestException {
    try {
      Path destinationDirectory = getDeterministicPathToTempDirectoryById(datasetId);
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

