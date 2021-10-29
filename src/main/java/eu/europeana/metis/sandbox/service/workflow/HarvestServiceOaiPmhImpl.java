package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.oaipmh.CloseableOaiClient;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvesterImpl;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvesterImpl.ConnectionClientFactory;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import eu.europeana.metis.harvesting.oaipmh.OaiRepository;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class HarvestServiceOaiPmhImpl implements HarvestServiceOaiPmh {

  private static final Logger LOGGER = LoggerFactory.getLogger(HarvestServiceOaiPmhImpl.class);

  private static ConnectionClientFactory CONNECTION_CLIENT_FACTORY;

  public List<ByteArrayInputStream> harvestOaiPmh(String endpoint, String setSpec, String prefix) {

    CloseableOaiClient client = CONNECTION_CLIENT_FACTORY.createConnectionClient(endpoint);
    OaiHarvester oaiHarvester = new OaiHarvesterImpl(CONNECTION_CLIENT_FACTORY);
    List<ByteArrayInputStream> records = new ArrayList<>();
    List<Pair<String, Exception>> exception = new ArrayList<>(1);

    try {
      OaiRecordHeaderIterator recordHeaderIterator =
          oaiHarvester.harvestRecordHeaders(new OaiHarvest("repoURL", "prefix", "spec "));

      recordHeaderIterator.forEach(r -> {
        OaiRepository oaiRepo = new OaiRepository("url", "prefix");
        try {
          var rec = oaiHarvester.harvestRecord(oaiRepo, r.getOaiIdentifier());
          records.add(new ByteArrayInputStream(rec.getRecord().readAllBytes()));
        } catch (HarvesterException | IOException e) {
          exception.add(new ImmutablePair<>(r.getOaiIdentifier(), e));
          return ReportingIteration.IterationResult.TERMINATE;
        }
        return ReportingIteration.IterationResult.CONTINUE;
      });
      if (!exception.isEmpty()) {
        throw new ServiceException("Error harvesting records " + exception.get(0).getKey(),
            exception.get(0).getValue());
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

}
