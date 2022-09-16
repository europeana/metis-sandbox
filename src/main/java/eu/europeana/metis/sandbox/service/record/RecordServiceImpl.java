package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
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
  public void setEuropeanaIdAndProviderId(Record recordToUpdate){
    String providerId = xmlRecordProcessorService.getProviderId(recordToUpdate.getContent());
    String europeanaId = EuropeanaIdCreator.constructEuropeanaIdString(providerId, recordToUpdate.getDatasetId());
    recordToUpdate.setEuropeanaId(europeanaId);
    recordToUpdate.setProviderId(providerId);
    recordRepository.updateEuropeanaIdAndProviderId(recordToUpdate.getRecordId(), europeanaId, providerId);
  }

  @Override
  @Transactional
  public void setContentTierAndMetadataTier(Record recordToUpdate, MediaTier contentTier, MetadataTier metadataTier) {
    recordRepository.updateContentTierAndMetadataTier(recordToUpdate.getRecordId(), contentTier.toString(),
            metadataTier.toString());
  }

  @Override
  @Transactional
  public void remove(String datasetId){
    recordRepository.deleteByDatasetId(datasetId);
  }

}
