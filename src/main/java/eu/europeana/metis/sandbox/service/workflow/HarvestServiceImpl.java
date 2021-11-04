package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpRecordIterator;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class HarvestServiceImpl implements HarvestService {


  @Override
  public List<ByteArrayInputStream> harvest(MultipartFile file) throws ServiceException {

    List<ByteArrayInputStream> records = new ArrayList<>();

    try {
      HarvesterFactory.createHttpHarvester()
          .harvestRecords(file.getInputStream(), CompressedFileExtension.ZIP, entry -> {
            final byte[] content = entry.getEntryContent().readAllBytes();
            records.add(new ByteArrayInputStream(content));
          });

    } catch (IOException | HarvesterException e) {
      throw new ServiceException("Error harvesting records ", e);
    }
    if (records.isEmpty()) {
      throw new ServiceException("Error records are empty ", null);
    }
    return records;
  }

  @Override
  public List<ByteArrayInputStream> harvest(String url) throws ServiceException {

    Path tempDir = null;
    List<ByteArrayInputStream> records = new ArrayList<>();

    try {
      final String prefix = UUID.randomUUID().toString();
      try {
        tempDir = Files.createTempDirectory(prefix);
      } catch (IOException e) {
        throw new ServiceException("Error creating temporary folder ", e);
      }
      // Perform the harvesting
      final HttpRecordIterator iterator = HarvesterFactory.createHttpHarvester()
          .harvestRecords(url, tempDir.toString());
      List<Pair<Path, Exception>> exception = new ArrayList<>(1);
      iterator.forEach(path -> {
        try (InputStream content = Files.newInputStream(path)) {
          records.add(new ByteArrayInputStream(content.readAllBytes()));
          return ReportingIteration.IterationResult.CONTINUE;
        } catch (IOException | RuntimeException e) {
          exception.add(new ImmutablePair<>(path, e));
          return ReportingIteration.IterationResult.TERMINATE;
        }
      });
      if (!exception.isEmpty()) {
        throw new ServiceException("Error processing path " + exception.get(0).getKey() + ".",
            exception.get(0).getValue());
      }
    } catch (HarvesterException e) {
      throw new ServiceException("Error harvesting records ", e);
    } finally {
      // Finally, attempt to delete the files
      if (tempDir != null) {
        try {
          FileUtils.deleteDirectory(tempDir.toFile());
        } catch (IOException e) {
          throw new ServiceException("Could not delete temporary folder ", e);
        }
      }
    }
    if (records.isEmpty()) {
      throw new ServiceException("Error records are empty ", null);
    }
    return records;
  }

}
