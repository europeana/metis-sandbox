package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
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
  @Transactional
  public void setEuropeanaIdAndProviderId(Record record){
    String providerId = xmlRecordProcessorService.getProviderId(record.getContent());
    String europeanaId = EuropeanaIdCreator.constructEuropeanaIdString(providerId, record.getDatasetId());
    record.setEuropeanaId(europeanaId);
    record.setProviderId(providerId);
    recordRepository.updateEuropeanaIdAndProviderId(record.getRecordId(), europeanaId, providerId);
  }

}
