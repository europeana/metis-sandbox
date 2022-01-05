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

  public RecordTierCalculationServiceImpl(RecordLogRepository recordLogRepository) {
    this.recordLogRepository = recordLogRepository;
  }

  @Override
  public RecordTierCalculationView calculateTiers(String recordId, String datasetId) {
    //Retrieve record from the database
    final RecordLogEntity recordLog = recordLogRepository.findRecordLog(recordId, datasetId, Step.MEDIA_PROCESS);

    RecordTierCalculationView recordTierCalculationView = null;
    if (Objects.nonNull(recordLog)) {
      // we need to also store the europeanaId during transformation in the database
      final RecordTierCalculationViewGenerator recordTierCalculationViewGenerator = new RecordTierCalculationViewGenerator("europeanaId", recordId,
          recordLog.getContent());
      recordTierCalculationView = recordTierCalculationViewGenerator.generate();
    }

    return recordTierCalculationView;
  }

}
