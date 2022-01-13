package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService.RecordIdType;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RecordServiceImpl implements RecordService {

  private final RecordRepository recordRepository;

  public RecordServiceImpl(RecordRepository recordRepository) {
    this.recordRepository = recordRepository;
  }

  @Override
  public String getProviderRecordString(RecordIdType recordIdType, String recordId, String datasetId)
      throws NoRecordFoundException {
    return Optional.ofNullable(getRecordEntity(recordIdType, recordId, datasetId)).map(
            RecordEntity::getContent)
        .orElseThrow(() -> new NoRecordFoundException(
            String.format("Record not found for RecordIdType: %s, recordId: %s, datasetId: %s", recordIdType, recordId,
                datasetId)));
  }

  @Override
  public RecordEntity getRecordEntity(RecordIdType recordIdType, String recordId, String datasetId) {
    final RecordEntity recordEntity;
    if (recordIdType == RecordIdType.EUROPEANA_ID) {
      recordEntity = recordRepository.findRecordEntityByEuropeanaIdAndDatasetId(recordId, datasetId);
    } else {
      recordEntity = recordRepository.findRecordEntityByIdAndDatasetId(Long.parseLong(recordId), datasetId);
    }
    return recordEntity;
  }

}
