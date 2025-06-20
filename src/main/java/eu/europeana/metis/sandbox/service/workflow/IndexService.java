package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.IndexingProperties;
import eu.europeana.indexing.exception.IndexerRelatedIndexingException;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.indexing.tiers.TierCalculationMode;
import eu.europeana.indexing.tiers.model.TierResults;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for indexing records and calculating tier-related data.
 */
@Slf4j
@Service
public class IndexService {

  private final Indexer indexer;
  private final IndexingProperties indexingProperties;

  /**
   * Constructor.
   *
   * @param indexer the indexer instance to process and calculate tier-related properties
   */
  public IndexService(Indexer indexer) {
    this.indexer = indexer;
    this.indexingProperties = new IndexingProperties(
        new Date(), false, Collections.emptyList(), false, TierCalculationMode.OVERWRITE);
  }

  /**
   * Indexes the given record and calculates tier-related properties.
   *
   * @param recordId the unique identifier of the record to be indexed
   * @param recordData the data of the record to be indexed
   * @return an {@link IndexingResult} containing the record data and tier results
   * @throws IndexingException if an error occurs during the indexing process
   */
  public IndexingResult indexRecord(String recordId, String recordData) throws IndexingException {
    log.info("Indexing: {}", recordId);

    InputStream inputStream = new ByteArrayInputStream(recordData.getBytes(StandardCharsets.UTF_8));
    TierResults tierResults = indexer.indexAndGetTierCalculations(inputStream, indexingProperties);

    if (tierResults == null || isAllDataNull(tierResults)) {
      throw new IndexerRelatedIndexingException(
          String.format("Something went wrong with tier calculations for record %s", recordId));
    }

    log.info("Indexed: {}", recordId);

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

  /**
   * Represents the result of an indexing operation, containing record data and the associated tier results.
   *
   * @param recordData the original record data that was indexed
   * @param tierResults the tier-related results derived from the indexing process
   */
  public record IndexingResult(String recordData, TierResults tierResults) {

  }
}

