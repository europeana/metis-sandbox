package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.http.HttpHarvesterImpl;
import eu.europeana.metis.harvesting.http.HttpRecordIterator;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


@Service
public class HarvestServiceImpl implements HarvestService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HarvestServiceImpl.class);

  @Override
  public List<ByteArrayInputStream> harvest(MultipartFile file) {
    List<ByteArrayInputStream> records = new ArrayList<>();
    HttpHarvester harvester = new HttpHarvesterImpl();
    try {
      harvester.harvestRecords(file.getInputStream(), CompressedFileExtension.ZIP, entry -> {
        final byte[] content = entry.getEntryContent().readAllBytes();
        records.add(new ByteArrayInputStream(content));
      });

    } catch (IOException | HarvesterException e) {
      throw new ServiceException(e.getMessage(), e);
    }

    if (records.isEmpty()) {
      throw new ServiceException("Provided file does not contain any records", null);
    }

    return records;
  }

  @Override
  public List<ByteArrayInputStream> harvest(String url) {

    Path tempDir = null;
    List<ByteArrayInputStream> records = new ArrayList<>();
    HttpHarvester harvester = new HttpHarvesterImpl();

    try {
      final String prefix = UUID.randomUUID().toString();
      try {
        tempDir = Files.createTempDirectory(prefix);
      } catch (IOException e) {
        throw new ServiceException(e.getMessage(), e);
      }
      // Now perform the harvesting
      final HttpRecordIterator iterator = harvester.harvestRecords(url, tempDir.toString());
      List<Pair<Path, Exception>> exception = new ArrayList<>(1);
      iterator.forEach(path -> {
        try (InputStream content = Files.newInputStream(path)) {
          records.add(new ByteArrayInputStream(content.readAllBytes()));
          return ReportingIteration.IterationResult.CONTINUE;
        } catch (IOException | RuntimeException e) {
          exception.add(new ImmutablePair<>(path, e));
          LOGGER.warn(e.getMessage(), e);
          return ReportingIteration.IterationResult.TERMINATE;
        }
      });
      if (!exception.isEmpty()) {
        throw new ServiceException("Could not process path " + exception.get(0).getKey() + ".",
            exception.get(0).getValue());
      }
    } catch (HarvesterException e) {
      LOGGER.warn(e.getMessage(), e);
    } finally {
      // Finally, attempt to delete the files.
      if (tempDir != null) {
        try {
          FileUtils.deleteDirectory(tempDir.toFile());
        } catch (IOException e) {
          LOGGER.warn("Could not delete temporary directory.", e);
        }
      }
    }
    if (records.isEmpty()) {
      throw new ServiceException("Provided file does not contain any records", null);
    }
    return records;
  }

}
