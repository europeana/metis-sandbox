package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService.RecordIdType;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordServiceImpl implements RecordService {

  private final RecordRepository recordRepository;
  private final XmlRecordProcessorService xmlRecordProcessorService;

  public RecordServiceImpl(RecordRepository recordRepository,
      XmlRecordProcessorService xmlRecordProcessorService) {
    this.recordRepository = recordRepository;
    this.xmlRecordProcessorService = xmlRecordProcessorService;
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
      recordEntity = recordRepository.findRecordEntityByProviderIdAndDatasetId(recordId, datasetId);
    }
    return recordEntity;
  }
  @Override
  @Transactional
  public void setEuropeanaIdAndProviderId(Record record){
    String providerId = xmlRecordProcessorService.getProviderId(record.getContent());
    String europeanaId = EuropeanaIdCreator.constructEuropeanaIdString(providerId, record.getDatasetId());
    record.setEuropeanaId(europeanaId);
    record.setProviderId(providerId);
    recordRepository.updateEuropeanaIdAndProviderId(record.getRecordId(), europeanaId, providerId);
  }

}
