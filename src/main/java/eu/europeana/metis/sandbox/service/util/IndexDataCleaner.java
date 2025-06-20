package eu.europeana.metis.sandbox.service.util;

import static java.util.Objects.requireNonNull;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.metis.sandbox.common.exception.DatasetIndexRemoveException;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Provides functionality to clean index data for a specific dataset.
 *
 * <p>Handles the removal of indexed data and ensures proper cleanup of resources.
 */
@Service
@AllArgsConstructor
public class IndexDataCleaner {

  private final Indexer publishIndexer;

  /**
   * Removes all indexed data for a specific dataset.
   *
   * @param datasetId the ID of the dataset to be removed. must not be null.
   */
  public void remove(String datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");

    try {
      publishIndexer.removeAll(datasetId, null);
    } catch (IndexingException e) {
      throw new DatasetIndexRemoveException(datasetId, e);
    }
  }

  /**
   * Releases resources held by the publishIndexer during shutdown.
   */
  @PreDestroy
  public void destroy() throws IOException {
    publishIndexer.close();
  }
}
