package eu.europeana.metis.sandbox.service.util;

import static java.util.Objects.requireNonNull;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.metis.sandbox.common.exception.DatasetIndexRemoveException;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
public class IndexDataCleanupService {

  private final Indexer publishIndexer;

  public IndexDataCleanupService(Indexer publishIndexer) {
    this.publishIndexer = publishIndexer;
  }

  public void remove(String datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");

    try {
      publishIndexer.removeAll(datasetId, null);
    } catch (IndexingException e) {
      throw new DatasetIndexRemoveException(datasetId, e);
    }
  }

  @PreDestroy
  public void destroy() throws IOException {
    publishIndexer.close();
  }
}
