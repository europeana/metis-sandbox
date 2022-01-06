package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.RecordTierCalculationViewGenerator;
import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
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
  private String providerRecordUrlTemplate;

  @Value("${sandbox.portal.publish.record-base-url}")
  private String portalPublishRecordBaseUrl;

  /**
   * Parameterized constructor
   *
   * @param recordLogService the record log repository
   */
  public RecordTierCalculationServiceImpl(RecordLogService recordLogService) {
    this.recordLogService = recordLogService;
  }

  @Override
  public RecordTierCalculationView calculateTiers(RecordIdType recordIdType, String recordId,
      String datasetId) {
    final RecordLogEntity recordLog = recordLogService.getRecordLogEntity(recordIdType, recordId, datasetId);

    RecordTierCalculationView recordTierCalculationView = null;
    if (Objects.nonNull(recordLog)) {
      final String portalPublishRecordUrl = new UriTemplate(this.portalPublishRecordBaseUrl).expand(recordLog.getEuropeanaId()).toString();
      final String providerRecordUrl = new UriTemplate(this.providerRecordUrlTemplate).expand(datasetId, recordId, recordIdType).toString();
      final RecordTierCalculationViewGenerator recordTierCalculationViewGenerator = new RecordTierCalculationViewGenerator(
          recordLog.getEuropeanaId(), recordLog.getRecordId(), recordLog.getContent(), portalPublishRecordUrl,
          providerRecordUrl);
      recordTierCalculationView = recordTierCalculationViewGenerator.generate();
    }

    return recordTierCalculationView;
  }

}
