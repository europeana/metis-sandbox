package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.RecordTierCalculationViewGenerator;
import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Service for calculating record tier statistics
 */
@Service
public class RecordTierCalculationServiceImpl implements RecordTierCaclulationService {

  private final RecordLogRepository recordLogRepository;

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
      final RecordTierCalculationViewGenerator recordTierCalculationViewGenerator = new RecordTierCalculationViewGenerator(
          recordLog.getEuropeanaId(), recordLog.getRecordId(), recordLog.getContent());
      recordTierCalculationView = recordTierCalculationViewGenerator.generate();
    }

    return recordTierCalculationView;
  }

}
