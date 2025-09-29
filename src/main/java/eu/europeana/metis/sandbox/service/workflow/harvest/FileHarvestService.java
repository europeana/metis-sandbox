package eu.europeana.metis.sandbox.service.workflow.harvest;

import static org.apache.tika.utils.StringUtils.isBlank;

import eu.europeana.metis.sandbox.common.FileType;
import eu.europeana.metis.sandbox.common.HarvestedRecord;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.utils.CompressedFileExtension;
import jakarta.validation.constraints.NotNull;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
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

  private static final String MAC_TEMP_FILE = ".DS_Store";
  private static final String MAC_TEMP_FOLDER = "__MACOSX";

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
  public HarvestIdentifiersResult<String> harvestExternalIdentifiers(@NotNull FileHarvestTarget fileHarvestTarget,
      Integer stepSize) {
    if (fileHarvestTarget.fileType().equals(FileType.XML)) {
      return new HarvestIdentifiersResult<>(List.of(fileHarvestTarget.fileName()), false);
    } else {
      return harvestIdentifiersFromCompressedArchive(fileHarvestTarget.fileContent(), stepSize);
    }
  }

  @Override
  public HarvestedRecord harvestRecord(@NotNull FileHarvestTarget fileHarvestTarget, String sourceRecordId)
      throws HarvestException {
    if (fileHarvestTarget.fileType().equals(FileType.XML)) {
      InputStream inputStream = new ByteArrayInputStream(fileHarvestTarget.fileContent());
      String recordData = getStringData(inputStream);
      return new HarvestedRecord(sourceRecordId, recordData);
    } else {
      return harvestRecordFromArchive(fileHarvestTarget.fileContent(), sourceRecordId);
    }
  }

  private HarvestIdentifiersResult<String> harvestIdentifiersFromCompressedArchive(byte[] fileContent, Integer stepSize)
      throws ServiceException {
    final int numberOfRecordsToStepInto = normalizeStepSize(stepSize);
    final List<String> result = new ArrayList<>();
    int recordCounter = 0;
    int skipCounter = 0;
    boolean recordLimitExceeded;

    try (InputStream bis = new ByteArrayInputStream(fileContent);
        BufferedInputStream buffered = new BufferedInputStream(bis);
        ArchiveInputStream<?> ais = new ArchiveStreamFactory().createArchiveInputStream(buffered)) {

      ArchiveEntry entry;
      while ((entry = ais.getNextEntry()) != null && recordCounter < maxAllowedRecords) {
        if (entry.isDirectory() || shouldSkip(entry.getName())) {
          continue;
        }

        if (skipCounter == 0) {
          log.debug("Processing entry {}", entry.getName());
          result.add(entry.getName());
          recordCounter++;
          skipCounter = numberOfRecordsToStepInto - 1;
        } else {
          skipCounter--;
        }
      }

      recordLimitExceeded = recordCounter >= maxAllowedRecords;
      if (recordLimitExceeded) {
        log.warn("Reached maximum number of records ({}) during harvesting.", maxAllowedRecords);
      }
    } catch (IOException e) {
      throw new ServiceException("Error harvesting records ", e);
    }
    return new HarvestIdentifiersResult<>(result, recordLimitExceeded);
  }

  private boolean shouldSkip(String name) {
    String nameLowerCase = name.toLowerCase(Locale.ROOT);

    return name.contains(MAC_TEMP_FOLDER)
        || name.endsWith(MAC_TEMP_FILE)
        || name.startsWith(".")
        || name.contains("/.")
        || Arrays.stream(CompressedFileExtension.values())
                 .anyMatch(ext -> nameLowerCase.endsWith(ext.getExtension().toLowerCase(Locale.ROOT)));
  }

  private HarvestedRecord harvestRecordFromArchive(byte[] fileContent, String sourceRecordId) {
    String recordData = null;
    try (InputStream bis = new ByteArrayInputStream(fileContent);
        BufferedInputStream buffered = new BufferedInputStream(bis);
        ArchiveInputStream<?> ais = new ArchiveStreamFactory().createArchiveInputStream(buffered)) {

      ArchiveEntry entry;
      while ((entry = ais.getNextEntry()) != null) {
        if (!entry.isDirectory() && entry.getName().equals(sourceRecordId)) {
          recordData = IOUtils.toString(ais, StandardCharsets.UTF_8);
          break;
        }
      }
    } catch (IOException e) {
      throw new ServiceException("Error harvesting records", e);
    }

    if (isBlank(recordData)) {
      throw new ServiceException("Record with ID '%s' not found in archive".formatted(sourceRecordId));
    }
    return new HarvestedRecord(sourceRecordId, recordData);
  }

  private String getStringData(InputStream inputStream) throws HarvestException {
    try {
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new HarvestException(e);
    }
  }
}

