package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.RecordTierCalculationViewGenerator;
import eu.europeana.indexing.tiers.view.ProcessingError;
import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.entity.RecordErrorLogEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import java.util.ArrayList;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriTemplate;

/**
 * Service for calculating record tier statistics
 */
@Service
public class RecordTierCalculationServiceImpl implements RecordTierCalculationService {

  private final RecordLogService recordLogService;
  @Value("${sandbox.dataset.provider-record-url-template}")
  private final String providerRecordUrlTemplate;
  @Value("${sandbox.portal.publish.record-base-url}")
  private final String portalPublishRecordBaseUrl;

  /**
   * Parameterized constructor
   *
   * @param recordLogService the record log repository
   * @param providerRecordUrlTemplate the provider record url template.
   * <p>
   * This string value should conform to {@link UriTemplate}.
   * </p>
   * @param portalPublishRecordBaseUrl the portal publish record base url
   */
  public RecordTierCalculationServiceImpl(RecordLogService recordLogService, String providerRecordUrlTemplate,
      String portalPublishRecordBaseUrl) {
    this.recordLogService = recordLogService;
    this.providerRecordUrlTemplate = providerRecordUrlTemplate;
    this.portalPublishRecordBaseUrl = portalPublishRecordBaseUrl;
  }

  @Override
  public RecordTierCalculationView calculateTiers(RecordIdType recordIdType, String recordId,
      String datasetId) throws NoRecordFoundException {
    final RecordLogEntity recordLog = recordLogService.getRecordLogEntity(recordIdType, recordId, datasetId);
    RecordTierCalculationView recordTierCalculationView;
    if (Objects.nonNull(recordLog)) {
      //Check if the record had failed
      final ArrayList<ProcessingError> processingErrors = new ArrayList<>();
      if (recordLog.getStatus() == Status.FAIL) {
        final RecordErrorLogEntity recordErrorLogEntity = recordLogService.getRecordErrorLogEntity(recordIdType, recordId,
            datasetId);
        processingErrors.add(new ProcessingError(recordErrorLogEntity.getMessage(), recordErrorLogEntity.getStackTrace()));
      }

      final String portalPublishRecordUrl = new UriTemplate(this.portalPublishRecordBaseUrl).expand(recordLog.getRecordId().getEuropeanaId())
                                                                                            .toString();
      final RecordTierCalculationViewGenerator recordTierCalculationViewGenerator = new RecordTierCalculationViewGenerator(
              recordLog.getRecordId().getEuropeanaId(), recordLog.getRecordId().getProviderId(), recordLog.getContent(),
              portalPublishRecordUrl, processingErrors);
      recordTierCalculationView = recordTierCalculationViewGenerator.generate();
    } else {
      throw new NoRecordFoundException(
          String.format("Record not found for RecordIdType: %s, recordId: %s, datasetId: %s", recordIdType, recordId, datasetId));
    }

    return recordTierCalculationView;
  }

}
