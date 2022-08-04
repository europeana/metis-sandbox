package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.IndexingProperties;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.metis.sandbox.common.exception.DatasetIndexRemoveException;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import java.io.IOException;
import java.util.Date;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Service
class IndexingServiceImpl implements IndexingService {

  private final Indexer publishIndexer;

  public IndexingServiceImpl(Indexer publishIndexer) {
    this.publishIndexer = publishIndexer;
  }

  @Override
  public RecordInfo index(Record recordToIndex) {
    requireNonNull(recordToIndex, "Record must not be null");
    Pair<MediaTier, MetadataTier> tierCalculations;

    try {
      tierCalculations = publishIndexer.indexAndGetTierCalculations(recordToIndex.getContentInputStream(),
              new IndexingProperties(new Date(), false, null, false, true));
    } catch (IndexingException ex) {
      throw new RecordProcessingException(recordToIndex.getProviderId(), ex);
    }

//TODO:    if(tierCalculations == null || (tierCalculations.getLeft() == null && tierCalculations.getRight() == null)){
//      throw new RecordProcessingException(recordToIndex.getProviderId(), new IndexerRelatedIndexingException(
//              String.format("Something wrong with tier calculations with record %s", recordToIndex.getProviderId())));
//    }

    return new RecordInfo(recordToIndex);
  }

  @Override
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
