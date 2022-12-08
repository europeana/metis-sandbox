package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.metis.sandbox.common.exception.RecordDuplicatedException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.repository.RecordJdbcRepository;
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
  private final RecordJdbcRepository recordJdbcRepository;
  private final XmlRecordProcessorService xmlRecordProcessorService;

  public RecordServiceImpl(RecordRepository recordRepository, RecordJdbcRepository recordJdbcRepository,
                           XmlRecordProcessorService xmlRecordProcessorService) {
    this.recordRepository = recordRepository;
    this.recordJdbcRepository = recordJdbcRepository;
    this.xmlRecordProcessorService = xmlRecordProcessorService;
  }

  @Override
  @Transactional
  public void setEuropeanaIdAndProviderId(Record recordToUpdate) {
    final String datasetId = recordToUpdate.getDatasetId();
    final String providerId = xmlRecordProcessorService.getProviderId(recordToUpdate.getContent());
    final String europeanaId = EuropeanaIdCreator.constructEuropeanaIdString(providerId, datasetId);

    final int updatedRecords = recordJdbcRepository.updateRecord(recordToUpdate.getRecordId(), europeanaId, providerId, datasetId);
    handleUpdateQueryResult(updatedRecords, providerId, europeanaId, datasetId, recordToUpdate);

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

  private void handleUpdateQueryResult(int updatedRecords, String providerId, String europeanaId, Record recordToUpdate){
    if (updatedRecords == 0) {
      LOGGER.debug("Duplicated ProviderId: {} | EuropeanaId: {}", providerId, europeanaId);
      throw new RecordDuplicatedException(
              String.format("Duplicated record has been found: ProviderId: %s | EuropeanaId: %s", providerId, europeanaId),
              String.valueOf(recordToUpdate.getRecordId()), providerId, europeanaId);

    } else if(updatedRecords == -1) {
      LOGGER.debug("Primary key in record table is corrupted (dataset_id,provider_id,europeana_id)");
      throw new ServiceException("primary key in record table is corrupted (dataset_id,provider_id,europeana_id)"
              + ". providerId & europeanaId updated multiple times", null);

    } else {
      LOGGER.debug("Setting ProviderId: {} | EuropeanaId: {}", providerId, europeanaId);
      recordToUpdate.setEuropeanaId(europeanaId);
      recordToUpdate.setProviderId(providerId);
    }
  }
}
