package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.http.HttpHarvesterImpl;
import eu.europeana.metis.harvesting.http.HttpRecordIterator;
import eu.europeana.metis.sandbox.common.exception.ServiceException;

import java.io.*;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


@Service
public class HarvestServiceImpl implements HarvestService {

  @Override
  public List<ByteArrayInputStream> harvest(MultipartFile file)  {

    List<ByteArrayInputStream> records;
    try {
      records = harvest(file.getInputStream());
    } catch (IOException e) {
      throw new ServiceException(e.getMessage(), e);
    }
    return records;
  }

  @Override
  public List<ByteArrayInputStream> harvest(String url) {

    List<ByteArrayInputStream> records;
    try {
      FileInputStream fis = new FileInputStream(url);
      records = harvest(fis);
    } catch (IOException e) {
      throw new ServiceException(e.getMessage(), e);
    }
    return records;
  }

  private List<ByteArrayInputStream> harvest(InputStream is){

    List<ByteArrayInputStream> records = new ArrayList<>();
    HttpHarvester harvester = new HttpHarvesterImpl();
    try {
      harvester.harvestRecords(is, CompressedFileExtension.ZIP, entry -> {
        final byte[] content = entry.getEntryContent().readAllBytes();
        records.add(new ByteArrayInputStream(content));
      });

    } catch (HarvesterException e) {
      throw new ServiceException(e.getMessage(), e);
    }

    if (records.isEmpty()) {
      throw new ServiceException("Provided file does not contain any records", null);
    }
    return records;
  }

}
