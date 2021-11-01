package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.http.HttpHarvesterImpl;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class HarvestServiceImpl implements HarvestService {

  @Override
  public List<ByteArrayInputStream> harvest(MultipartFile file) throws ServiceException {

    List<ByteArrayInputStream> records;
    try {
      records = harvest(file.getInputStream());
    } catch (IOException e) {
      throw new ServiceException("Error harvesting records from file "+file.getName(), e);
    }
    return records;
  }

  @Override
  public List<ByteArrayInputStream> harvest(String url) throws ServiceException {
    List<ByteArrayInputStream> records;
    try(FileInputStream fis = new FileInputStream(url)) {
      records = harvest(fis);
    } catch (IOException e) {
      throw new ServiceException("Error harvesting records from "+url, e);
    }
    return records;
  }

  private List<ByteArrayInputStream> harvest(InputStream is) throws ServiceException {

    List<ByteArrayInputStream> records = new ArrayList<>();
    HttpHarvester harvester = new HttpHarvesterImpl();
    try {
      harvester.harvestRecords(is, CompressedFileExtension.ZIP, entry -> {
        final byte[] content = entry.getEntryContent().readAllBytes();
        records.add(new ByteArrayInputStream(content));
      });

    } catch (HarvesterException e) {
      throw new ServiceException("Error harvesting records ", e);
    }

    if (records.isEmpty()) {
      throw new ServiceException("Provided file does not contain any records", null);
    }
    return records;
  }

}
