package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.http.HttpHarvesterImpl;
import eu.europeana.metis.harvesting.oaipmh.CloseableOaiClient;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvesterImpl;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvesterImpl.ConnectionClientFactory;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import eu.europeana.metis.harvesting.oaipmh.OaiRepository;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class HarvestServiceImpl implements HarvestService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HarvestServiceImpl.class);

  private static ConnectionClientFactory CONNECTION_CLIENT_FACTORY;

  @Override
  public List<ByteArrayInputStream> harvest(MultipartFile file) throws ServiceException {

    List<ByteArrayInputStream> records;
    try {
      records = harvest(file.getInputStream());
    } catch (IOException e) {
      throw new ServiceException("Error harvesting records from file " + file.getName(), e);
    }
    return records;
  }

  @Override
  public List<ByteArrayInputStream> harvest(String url) throws ServiceException {
    List<ByteArrayInputStream> records;
    try (FileInputStream fis = new FileInputStream(url)) {
      records = harvest(fis);
    } catch (IOException e) {
      throw new ServiceException("Error harvesting records from " + url, e);
    }
    return records;
  }

  @Override
  public List<ByteArrayInputStream> harvestOaiPmh(String endpoint, String setSpec, String prefix) {

    CloseableOaiClient client = CONNECTION_CLIENT_FACTORY.createConnectionClient(endpoint);
    OaiHarvester oaiHarvester = new OaiHarvesterImpl(CONNECTION_CLIENT_FACTORY);
    List<ByteArrayInputStream> records = new ArrayList<>();
    List<Pair<String, Exception>> exceptions = new ArrayList<>(1);

    try {
      OaiRecordHeaderIterator recordHeaderIterator =
          oaiHarvester.harvestRecordHeaders(new OaiHarvest(endpoint, prefix, setSpec));

      recordHeaderIterator.forEach(r -> {
        OaiRepository oaiRepo = new OaiRepository(endpoint, prefix);
        try {
          var rec = oaiHarvester.harvestRecord(oaiRepo, r.getOaiIdentifier());
          records.add(new ByteArrayInputStream(rec.getRecord().readAllBytes()));
        } catch (HarvesterException | IOException e) {
          exceptions.add(new ImmutablePair<>(r.getOaiIdentifier(), e));
          return ReportingIteration.IterationResult.TERMINATE;
        }
        return ReportingIteration.IterationResult.CONTINUE;
      });
      if (!exceptions.isEmpty()) {
        throw new ServiceException("Error harvesting records " + exceptions.get(0).getKey(),
            exceptions.get(0).getValue());
      }
    } catch (HarvesterException e) {
      throw new ServiceException("Error harvesting records ", e);
    } finally {
      try {
        client.close();
      } catch (IOException e) {
        LOGGER.warn("Error closing OAI client ", e);
      }
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
