package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.metis.sandbox.common.exception.RecordDuplicatedException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to modify Records in database
 */
@Service
public class RecordServiceImpl implements RecordService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecordServiceImpl.class);
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

    final int updatedRecords = recordRepository.updateEuropeanaIdAndProviderId(recordToUpdate.getRecordId(), europeanaId, providerId, datasetId);
    if (updatedRecords == 0) {
      throw new RecordDuplicatedException("ProviderId: " + providerId + " | EuropeanaId: " + europeanaId + " is duplicated.");
    } else {
      recordToUpdate.setEuropeanaId(europeanaId);
      recordToUpdate.setProviderId(providerId);
    }
  }

  @Override
  @Transactional
  public void setContentTierAndMetadataTier(Record recordToUpdate, MediaTier contentTier, MetadataTier metadataTier) {
    recordRepository.updateContentTierAndMetadataTier(recordToUpdate.getRecordId(), contentTier.toString(),
        metadataTier.toString());
  }

  @Override
  @Transactional
  public void remove(String datasetId) {
    recordRepository.deleteByDatasetId(datasetId);
  }
}
