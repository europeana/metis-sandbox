package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import eu.europeana.metis.harvesting.oaipmh.OaiRepository;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class HarvestServiceImpl implements HarvestService {

  private final HttpHarvester harvester;

  private final OaiHarvester harvesterOai;

  @Value("${sandbox.dataset.max-size}")
  private int maxRecords;

  @Autowired
  public HarvestServiceImpl(HttpHarvester harvester,
      OaiHarvester harvesterOai) {
    this.harvester = harvester;
    this.harvesterOai = harvesterOai;
  }

  @Override
  public Pair<AtomicBoolean, List<ByteArrayInputStream>> harvestZipMultipartFile(MultipartFile file)
      throws ServiceException {

    Pair<AtomicBoolean, List<ByteArrayInputStream>> pairResult;

    try {
      pairResult = this.harvest(file.getInputStream());
    } catch (IOException e) {
      throw new ServiceException("Error harvesting records from file " + file.getName(), e);
    }
    return pairResult;
  }

  @Override
  public Pair<AtomicBoolean, List<ByteArrayInputStream>> harvestZipUrl(String url) throws ServiceException {

    Pair<AtomicBoolean, List<ByteArrayInputStream>> pairResult;

    try (InputStream input = new URL(url).openStream()) {
      pairResult = this.harvest(input);
    } catch (IOException e) {
      throw new ServiceException("Error harvesting records from " + url, e);
    }
    return pairResult;
  }

  @Override
  public Pair<AtomicBoolean, List<ByteArrayInputStream>> harvestOaiPmhEndpoint(String endpoint, String setSpec,
      String prefix)
      throws ServiceException {

    List<ByteArrayInputStream> records = new ArrayList<>();
    List<Pair<String, Exception>> exceptions = new ArrayList<>();
    AtomicBoolean hasReachedRecordLimit = new AtomicBoolean(false);

    try (OaiRecordHeaderIterator recordHeaderIterator = harvesterOai
        .harvestRecordHeaders(new OaiHarvest(endpoint, prefix, setSpec))) {

      OaiRepository oaiRepository = new OaiRepository(endpoint, prefix);

      recordHeaderIterator.forEach(recordHeader -> {
        try {
          if(records.size() == maxRecords){
            hasReachedRecordLimit.set(true);
          } else {
            OaiRecord oaiRecord = harvesterOai
                .harvestRecord(oaiRepository, recordHeader.getOaiIdentifier());
            records.add(new ByteArrayInputStream(oaiRecord.getRecord().readAllBytes()));
          }
        } catch (HarvesterException | IOException e) {
          exceptions.add(new ImmutablePair<>(recordHeader.getOaiIdentifier(), e));
          return ReportingIteration.IterationResult.TERMINATE;
        }
        return ReportingIteration.IterationResult.CONTINUE;
      });
      if (!exceptions.isEmpty()) {
        throw new ServiceException("Error processing " + exceptions.get(0).getKey(),
            exceptions.get(0).getValue());
      }
    } catch (HarvesterException | IOException e) {
      throw new ServiceException("Error harvesting records ", e);
    }
    if (records.isEmpty()) {
      throw new ServiceException("Error records are empty ", null);
    }
    return new ImmutablePair<>(hasReachedRecordLimit,records);
  }

  private Pair<AtomicBoolean, List<ByteArrayInputStream>> harvest(InputStream inputStream) throws ServiceException {

    List<ByteArrayInputStream> records = new ArrayList<>();
    AtomicBoolean hasReachedRecordLimit = new AtomicBoolean(false);

    try {
      harvester.harvestRecords(inputStream, CompressedFileExtension.ZIP, entry -> {
        if(records.size() == maxRecords){
          hasReachedRecordLimit.set(true);
        } else {
          final byte[] content = entry.getEntryContent().readAllBytes();
          records.add(new ByteArrayInputStream(content));
        }
      });

    } catch (HarvesterException e) {
      throw new ServiceException("Error harvesting records ", e);
    }

    if (records.isEmpty()) {
      throw new ServiceException("Provided file does not contain any records", null);
    }
    return new ImmutablePair<>(hasReachedRecordLimit,records);
  }

}
