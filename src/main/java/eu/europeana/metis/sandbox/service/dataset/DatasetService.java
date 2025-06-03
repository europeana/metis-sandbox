package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.FileHarvestingDTO;
import eu.europeana.metis.sandbox.dto.HarvestingParametersDTO;
import eu.europeana.metis.sandbox.dto.HttpHarvestingDTO;
import eu.europeana.metis.sandbox.dto.OAIPmhHarvestingDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.HarvestingParameterEntity;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import eu.europeana.metis.sandbox.repository.DatasetRepository.DatasetIdProjection;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatasetService {

  private final DatasetRepository datasetRepository;
  private final HarvestingParameterService harvestingParameterService;

  public DatasetService(DatasetRepository datasetRepository, HarvestingParameterService harvestingParameterService) {
    this.datasetRepository = datasetRepository;
    this.harvestingParameterService = harvestingParameterService;
  }

  @Transactional
  public String createEmptyDataset(WorkflowType workflowType, String datasetName, String createdById, Country country,
      Language language,
      String xsltToEdmExternal) {
    requireNonNull(datasetName, "Dataset name must not be null");
    requireNonNull(country, "Country must not be null");
    requireNonNull(language, "Language must not be null");

    DatasetEntity entity = saveNewDatasetInDatabase(
        new DatasetEntity(workflowType, datasetName, createdById, null, language, country, false),
        xsltToEdmExternal);

    return String.valueOf(entity.getDatasetId());

  }

  public List<String> findDatasetIdsByCreatedBefore(int days) {
    ZonedDateTime date = ZonedDateTime.now()
                                      .truncatedTo(ChronoUnit.DAYS)
                                      .minusDays(days);

    try {
      return datasetRepository.findByCreatedDateBefore(date).stream()
                              .map(DatasetIdProjection::getDatasetId)
                              .map(Object::toString)
                              .toList();
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error getting datasets older than %s days. ", days), e);
    }
  }

  @Transactional
  public void remove(String datasetId) {
    try {
      datasetRepository.deleteById(Integer.valueOf(datasetId));
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error removing dataset id: [%s]. ", datasetId), e);
    }
  }

  @Transactional
  public void updateNumberOfTotalRecord(String datasetId, Long numberOfRecords) {
    datasetRepository.updateRecordsQuantity(Integer.parseInt(datasetId), numberOfRecords);
  }

  @Transactional
  public void setRecordLimitExceeded(String datasetId) {
    datasetRepository.setRecordLimitExceeded(Integer.parseInt(datasetId));
  }

  public boolean isXsltPresent(String datasetId) {
    return datasetRepository.isXsltPresent(Integer.parseInt(datasetId)) != 0;
  }

  public DatasetInfoDTO getDatasetInfo(String datasetId) {
    DatasetEntity datasetEntity = datasetRepository.findById(Integer.valueOf(datasetId))
                                                   .orElseThrow(() -> new InvalidDatasetException(datasetId));
    return new DatasetInfoDTO.Builder()
        .datasetId(datasetId)
        .datasetName(datasetEntity.getDatasetName())
        .createdById(datasetEntity.getCreatedById())
        .creationDate(datasetEntity.getCreatedDate())
        .language(datasetEntity.getLanguage())
        .country(datasetEntity.getCountry())
        .harvestingParametricDto(getHarvestingParameterDto(datasetId))
        .transformedToEdmExternal(isXsltPresent(datasetId))
        .build();
  }

  private DatasetEntity saveNewDatasetInDatabase(DatasetEntity datasetEntityToSave, String xsltToEdmExternal) {
    if (StringUtils.isNotBlank(xsltToEdmExternal)) {
      datasetEntityToSave.setXsltToEdmExternal(xsltToEdmExternal);
    }

    try {
      return datasetRepository.save(datasetEntityToSave);
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error creating dataset: [%s]. ", datasetEntityToSave.getDatasetName()), e);
    }
  }

  private HarvestingParametersDTO getHarvestingParameterDto(String datasetId) {
    HarvestingParameterEntity entity = harvestingParameterService.getDatasetHarvestingParameters(datasetId);

    return switch (entity.getProtocol()) {
      case FILE -> new FileHarvestingDTO(entity.getFileName(), entity.getFileType());
      case HTTP -> new HttpHarvestingDTO(entity.getUrl());
      case OAI_PMH -> new OAIPmhHarvestingDTO(entity.getUrl(), entity.getSetSpec(), entity.getMetadataFormat());
    };
  }
}
