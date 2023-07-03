package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.model.TierResults;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.RecordDuplicatedException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.dto.RecordTiersInfoDto;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordJdbcRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
  public List<RecordTiersInfoDto> getRecordsTiers(String datasetId){
    List<RecordEntity> recordEntities = recordRepository.findByDatasetId(datasetId);

    if(recordEntities.isEmpty()){
      throw new InvalidDatasetException(datasetId);
    }

    return recordEntities.stream()
            .filter(this::areAllTierValuesNotNullOrEmpty)
            .map(RecordTiersInfoDto::new)
            .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void setEuropeanaIdAndProviderId(Record recordToUpdate) {
    final String datasetId = recordToUpdate.getDatasetId();
    final String providerId = xmlRecordProcessorService.getProviderId(recordToUpdate.getContent());
    final String europeanaId = EuropeanaIdCreator.constructEuropeanaIdString(providerId, datasetId);

    final int updatedRecords = recordJdbcRepository.updateRecord(recordToUpdate.getRecordId(), europeanaId, providerId, datasetId);
    handleUpdateQueryResult(updatedRecords, providerId, europeanaId, recordToUpdate);
  }

  @Override
  @Transactional
  public void setTierResults(Record recordToUpdate, TierResults tierResults) {
    recordRepository.updateRecordWithTierResults(recordToUpdate.getRecordId(),
            tierResults.getMediaTier().toString(),
            tierResults.getMetadataTier().toString(),
            tierResults.getContentTierBeforeLicenseCorrection().toString(),
            tierResults.getMetadataTierLanguage().toString(),
            tierResults.getMetadataTierEnablingElements().toString(),
            tierResults.getMetadataTierContextualClasses().toString(),
            tierResults.getLicenseType().toString());
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
              String.format("Duplicated record has been found: ProviderId: %s | EuropeanaId: %s", providerId, europeanaId));

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

  private boolean areAllTierValuesNotNullOrEmpty(RecordEntity recordEntity){
    return !((recordEntity.getContentTier() == null || StringUtils.isEmpty(recordEntity.getContentTier())) &&
            (recordEntity.getMetadataTier() == null || StringUtils.isEmpty(recordEntity.getMetadataTier())) &&
            (recordEntity.getContentTierBeforeLicenseCorrection() == null ||
                    StringUtils.isEmpty(recordEntity.getContentTierBeforeLicenseCorrection())) &&
            (recordEntity.getMetadataTierLanguage() == null || StringUtils.isEmpty(recordEntity.getMetadataTierLanguage())) &&
            (recordEntity.getMetadataTierEnablingElements() == null || StringUtils.isEmpty(recordEntity.getMetadataTierEnablingElements())) &&
            (recordEntity.getMetadataTierContextualClasses() == null || StringUtils.isEmpty(recordEntity.getMetadataTierContextualClasses())) &&
            (recordEntity.getLicense() == null));
  }
}
