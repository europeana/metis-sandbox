package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.RecordTierCalculationViewGenerator;
import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for calculating record tier statistics
 */
@Service
public class RecordTierCalculationServiceImpl implements RecordTierCaclulationService {

  private static final String JSON_SUFFIX = ".json";
  private final RecordLogRepository recordLogRepository;

  @Value("${sandbox.portal.preview.record-base-url}")
  private String portalPreviewRecordBaseUrl;

  @Value("${sandbox.portal.publish.record-base-url}")
  private String portalPublishRecordBaseUrl;

  /**
   * Parameterized constructor
   *
   * @param recordLogRepository the record log repository
   */
  public RecordTierCalculationServiceImpl(RecordLogRepository recordLogRepository) {
    this.recordLogRepository = recordLogRepository;
  }

  @Override
  public RecordTierCalculationView calculateTiers(RecordIdType recordIdType, String recordId,
      String datasetId) {
    //Retrieve record from the database
    final RecordLogEntity recordLog;
    if (recordIdType == RecordIdType.EUROPEANA_ID) {
      recordLog = recordLogRepository.findRecordLogByEuropeanaIdAndDatasetIdAndStep(recordId, datasetId,
          Step.MEDIA_PROCESS);
    } else {
      recordLog = recordLogRepository.findRecordLogByRecordIdAndDatasetIdAndStep(recordId, datasetId,
          Step.MEDIA_PROCESS);
    }

    RecordTierCalculationView recordTierCalculationView = null;
    if (Objects.nonNull(recordLog)) {
      final String portalPublishRecordUrl = portalPublishRecordBaseUrl + recordLog.getEuropeanaId() + JSON_SUFFIX;
      final RecordTierCalculationViewGenerator recordTierCalculationViewGenerator = new RecordTierCalculationViewGenerator(
          recordLog.getEuropeanaId(), recordLog.getRecordId(), recordLog.getContent(), portalPublishRecordUrl);
      recordTierCalculationView = recordTierCalculationViewGenerator.generate();
    }

    return recordTierCalculationView;
  }

}
