package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordDuplicatedException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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
      LOGGER.debug("Duplicated ProviderId: {} | EuropeanaId: {}", providerId, europeanaId);
      throw new RecordDuplicatedException("Duplicated record has been found.",  String.valueOf(recordToUpdate.getRecordId()), providerId, europeanaId);
    } else if (updatedRecords == 1) {
      LOGGER.debug("Setting ProviderId: {} | EuropeanaId: {}", providerId, europeanaId);
      recordToUpdate.setEuropeanaId(europeanaId);
      recordToUpdate.setProviderId(providerId);
    } else {
      LOGGER.debug("Primary key in record table is corruped (dataset_id,provider_id,europeana_id)");
      throw new ServiceException("primary key in record table is corrupted (dataset_id,provider_id,europeana_id)"
          + ". providerId & europeanaId updated multiple times", null);
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

  private boolean isDuplicatedByProviderId(RecordEntity recordEntity, String datasetId) {
    RecordEntity recordFound = recordRepository.findByProviderIdAndDatasetId(recordEntity.getProviderId(), datasetId);
    return recordFound != null;
  }

  private RecordInfo handleDuplicated(String providerId, Step step, Record.RecordBuilder recordToHarvest) {
    RecordError recordErrorCreated = new RecordError("Duplicated record", "Record already registered");
//    saveErrorWhileHarvesting(recordToHarvest, providerId, step, new RuntimeException(recordErrorCreated.getMessage()));
    return null;
  }
}
