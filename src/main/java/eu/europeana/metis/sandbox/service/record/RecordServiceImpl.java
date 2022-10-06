package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.metis.sandbox.common.exception.RecordDuplicatedException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to modify Records in database
 */
@Service
public class RecordServiceImpl implements RecordService {

  private final RecordRepository recordRepository;
  private final XmlRecordProcessorService xmlRecordProcessorService;

  public RecordServiceImpl(RecordRepository recordRepository, XmlRecordProcessorService xmlRecordProcessorService) {
    this.recordRepository = recordRepository;
    this.xmlRecordProcessorService = xmlRecordProcessorService;
  }

  @Override
  @Transactional
  public void setEuropeanaIdAndProviderId(Record recordToUpdate) {
    final String datasetId = recordToUpdate.getDatasetId();
    final String providerId = xmlRecordProcessorService.getProviderId(recordToUpdate.getContent());
    final String europeanaId = EuropeanaIdCreator.constructEuropeanaIdString(providerId, datasetId);

    RecordEntity recordEntityProviderCheck = recordRepository.findByProviderIdAndDatasetId(providerId, datasetId);
    RecordEntity recordEntityEuropeanaCheck = recordRepository.findByEuropeanaIdAndDatasetId(europeanaId, datasetId);

    if (recordEntityProviderCheck == null && recordEntityEuropeanaCheck == null) {
      recordToUpdate.setEuropeanaId(europeanaId);
      recordToUpdate.setProviderId(providerId);
      recordRepository.updateEuropeanaIdAndProviderId(recordToUpdate.getRecordId(), europeanaId, providerId);
    } else if (recordEntityProviderCheck != null && recordEntityEuropeanaCheck != null) {
      throw new RecordDuplicatedException("ProviderId: " + providerId + " and EuropeanaId: " + europeanaId + " are duplicated.");
    } else if (recordEntityProviderCheck != null) {
      throw new RecordDuplicatedException("ProviderId: " + providerId + " is duplicated.");
    } else if (recordEntityEuropeanaCheck != null) {
      throw new RecordDuplicatedException("EuropeanaId: " + europeanaId + " is duplicated.");
    }
  }

  @Override
  @Transactional
  public void setContentTierAndMetadataTier(Record recordToUpdate, MediaTier contentTier, MetadataTier metadataTier) {
    recordRepository.updateContentTierAndMetadataTier(recordToUpdate.getRecordId(), contentTier.toString(), metadataTier.toString());
  }

  @Override
  @Transactional
  public void remove(String datasetId) {
    recordRepository.deleteByDatasetId(datasetId);
  }
}
