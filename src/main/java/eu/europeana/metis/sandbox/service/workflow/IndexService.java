package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.IndexingProperties;
import eu.europeana.indexing.exception.IndexerRelatedIndexingException;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.indexing.tiers.TierCalculationMode;
import eu.europeana.indexing.tiers.model.TierResults;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IndexService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Indexer indexer;
  private final IndexingProperties indexingProperties;

  public IndexService(Indexer indexer) {
    this.indexer = indexer;
    this.indexingProperties = new IndexingProperties(
        new Date(), false, Collections.emptyList(), false, TierCalculationMode.OVERWRITE);
  }

  public IndexingResult indexRecord(String recordId, String recordData) throws IndexingException {
    LOGGER.info("Indexing: {}", recordId);

    InputStream inputStream = new ByteArrayInputStream(recordData.getBytes(StandardCharsets.UTF_8));
    TierResults tierResults = indexer.indexAndGetTierCalculations(inputStream, indexingProperties);

    if (tierResults == null || isAllDataNull(tierResults)) {
      throw new IndexerRelatedIndexingException(
          String.format("Something went wrong with tier calculations for record %s", recordId));
    }

    LOGGER.info("Indexed: {}", recordId);

    return new IndexingResult(recordData, tierResults);
  }

  private boolean isAllDataNull(TierResults tierResultsToCheck) {
    return tierResultsToCheck.getMediaTier() == null &&
        tierResultsToCheck.getMetadataTier() == null &&
        tierResultsToCheck.getContentTierBeforeLicenseCorrection() == null &&
        tierResultsToCheck.getMetadataTierLanguage() == null &&
        tierResultsToCheck.getMetadataTierContextualClasses() == null &&
        tierResultsToCheck.getMetadataTierEnablingElements() == null &&
        tierResultsToCheck.getLicenseType() == null;
  }

  public record IndexingResult(String recordData, TierResults tierResults) {

  }
}

