package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;

import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import eu.europeana.metis.harvesting.oaipmh.OaiRepository;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class HarvestServiceImpl implements HarvestService {

  private static final HttpHarvester harvester = HarvesterFactory.createHttpHarvester();
  private static final OaiHarvester harvesterOai = HarvesterFactory.createOaiHarvester();

  @Override
  public List<ByteArrayInputStream> harvest(MultipartFile file) throws ServiceException {

    List<ByteArrayInputStream> records;

    try {
      records = this.harvest(file.getInputStream());
    } catch (IOException e) {
      throw new ServiceException("Error harvesting records from file " + file.getName(), e);
    }
    return records;
  }

  @Override
  public List<ByteArrayInputStream> harvest(String url) throws ServiceException {

    List<ByteArrayInputStream> records;

    try (InputStream input = new URL(url).openStream()) {
      records = this.harvest(input);
    } catch (IOException e) {
      throw new ServiceException("Error harvesting records from " + url, e);
    }
    return records;
  }

  @Override
  public List<ByteArrayInputStream> harvest(String endpoint, String setSpec, String prefix) throws ServiceException {

    List<ByteArrayInputStream> records = new ArrayList<>();
    List<Pair<String, Exception>> exceptions = new ArrayList<>();

    try {
      OaiRecordHeaderIterator recordHeaderIterator = harvesterOai
          .harvestRecordHeaders(new OaiHarvest(endpoint, prefix, setSpec));
      OaiRepository oaiRepo = new OaiRepository(endpoint, prefix);

      recordHeaderIterator.forEach(rh -> {
        try {
          var rec = harvesterOai
              .harvestRecord(oaiRepo, rh.getOaiIdentifier());
          records.add(new ByteArrayInputStream(rec.getRecord().readAllBytes()));
        } catch (HarvesterException | IOException e) {
          exceptions.add(new ImmutablePair<>(rh.getOaiIdentifier(), e));
          return ReportingIteration.IterationResult.TERMINATE;
        }
        return ReportingIteration.IterationResult.CONTINUE;
      });
      if (!exceptions.isEmpty()) {
        throw new ServiceException("Error processing " + exceptions.get(0).getKey(),
            exceptions.get(0).getValue());
      }
    } catch (HarvesterException e) {
      throw new ServiceException("Error harvesting records ", e);
    }
    if (records.isEmpty()) {
      throw new ServiceException("Error records are empty ", null);
    }
    return records;
  }

  private List<ByteArrayInputStream> harvest(InputStream is) throws ServiceException {

    List<ByteArrayInputStream> records = new ArrayList<>();

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
