package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.IndexingProperties;
import eu.europeana.indexing.exception.IndexerRelatedIndexingException;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.indexing.tiers.model.TierResults;
import eu.europeana.metis.sandbox.common.exception.DatasetIndexRemoveException;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.record.RecordService;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
class IndexingServiceImpl implements IndexingService {

  private final Indexer publishIndexer;
  private final RecordService recordService;

  public IndexingServiceImpl(Indexer publishIndexer, RecordService recordService) {
    this.publishIndexer = publishIndexer;
    this.recordService = recordService;
  }

  @Override
  public RecordInfo index(Record recordToIndex) {
    requireNonNull(recordToIndex, "Record must not be null");
    TierResults tierCalculations;

    try {
      tierCalculations = publishIndexer.indexAndGetTierCalculations(recordToIndex.getContentInputStream(),
              //TODO: Refactor object IndexingProperties constructor to make it readable
              //TODO: "Too many different boolean parameters indicates many possibilities to create this object."
              new IndexingProperties(new Date(), false, null, false, true));
    } catch (IndexingException ex) {
      throw new RecordProcessingException(recordToIndex.getProviderId(), ex);
    }

    if(tierCalculations == null || isAllDataNull(tierCalculations)){
      throw new RecordProcessingException(recordToIndex.getProviderId(), new IndexerRelatedIndexingException(
              String.format("Something went wrong with tier calculations with record %s", recordToIndex.getProviderId())));
    }

    recordService.setTierResults(recordToIndex, tierCalculations);

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

  private boolean isAllDataNull(TierResults tierResultsToCheck){
    return tierResultsToCheck.getMediaTier() == null &&
            tierResultsToCheck.getMetadataTier() == null &&
            tierResultsToCheck.getContentTierBeforeLicenseCorrection() == null &&
            tierResultsToCheck.getMetadataTierLanguage() == null &&
            tierResultsToCheck.getMetadataTierContextualClasses() == null &&
            tierResultsToCheck.getMetadataTierEnablingElements() == null &&
            tierResultsToCheck.getLicenseType() == null;
  }
}
