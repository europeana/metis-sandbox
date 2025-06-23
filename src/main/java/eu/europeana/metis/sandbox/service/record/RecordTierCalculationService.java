package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.RecordTierCalculationViewGenerator;
import eu.europeana.indexing.tiers.view.ProcessingError;
import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordError;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordErrorRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import java.util.ArrayList;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriTemplate;

/**
 * Service for calculating record tier statistics
 */
@Service
public class RecordTierCalculationService {

  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordErrorRepository executionRecordErrorRepository;
  @Value("${sandbox.portal.publish.record-base-url}")
  private final String portalPublishRecordBaseUrl;

  /**
   * Constructor.
   *
   * @param executionRecordRepository repository for handling execution records
   * @param executionRecordErrorRepository repository for handling execution record errors
   * @param portalPublishRecordBaseUrl base URL for publishing records in the portal
   */
  public RecordTierCalculationService(ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordErrorRepository executionRecordErrorRepository,
      @Qualifier("portalPublishRecordBaseUrl") String portalPublishRecordBaseUrl) {
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordErrorRepository = executionRecordErrorRepository;
    this.portalPublishRecordBaseUrl = portalPublishRecordBaseUrl;
  }

  /**
   * Calculates the tier statistics of a record based on its dataset and record identifiers.
   *
   * <p>Retrieves the associated execution record, associated errors, and generates
   * the tier calculation view for the given record.
   *
   * @param recordId the unique identifier of the record
   * @param datasetId the unique identifier of the dataset to which the record belongs
   * @return a view containing the calculated tier statistics of the record
   * @throws NoRecordFoundException if the specified record or dataset combination is not found
   */
  public RecordTierCalculationView calculateTiers(String recordId, String datasetId) throws NoRecordFoundException {
    ExecutionRecord executionRecord = executionRecordRepository.findByIdentifier_DatasetIdAndIdentifier_RecordIdAndIdentifier_ExecutionName(
        datasetId, recordId, FullBatchJobType.MEDIA.name());
    RecordTierCalculationView recordTierCalculationView;
    final ArrayList<ProcessingError> processingErrors = new ArrayList<>();
    if (Objects.isNull(executionRecord)) {
      throw new NoRecordFoundException(
          String.format("Record not found for recordId: %s, datasetId: %s", recordId, datasetId));
    } else {
      final String portalPublishRecordUrl =
          new UriTemplate(this.portalPublishRecordBaseUrl).expand(executionRecord.getIdentifier().getRecordId()).toString();
      final RecordTierCalculationViewGenerator recordTierCalculationViewGenerator = new RecordTierCalculationViewGenerator(
          executionRecord.getIdentifier().getRecordId(), executionRecord.getIdentifier().getRecordId(),
          executionRecord.getRecordData(),
          portalPublishRecordUrl, processingErrors);

      ExecutionRecordError executionRecordError =
          executionRecordErrorRepository.findByIdentifier_DatasetIdAndIdentifier_RecordIdAndIdentifier_ExecutionName(
              datasetId, recordId, FullBatchJobType.MEDIA.name());
      if (Objects.nonNull(executionRecordError)) {
        processingErrors.add(
            new ProcessingError(executionRecordError.getException(), executionRecordError.getException()));
      }
      recordTierCalculationView = recordTierCalculationViewGenerator.generate();
    }
    return recordTierCalculationView;
  }
}
