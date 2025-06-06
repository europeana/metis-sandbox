package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.HarvestParametersConverter;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.harvest.HarvestParametersDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.HarvestingParameterRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class to access harvesting parameters table in the database
 */
@Service
public class HarvestParameterService {

  private final HarvestingParameterRepository harvestingParameterRepository;
  private final DatasetRepository datasetRepository;

  /**
   * Constructor
   *
   * @param harvestingParameterRepository The repository that connected to the table harvesting parameters
   * @param datasetRepository The repository that connects to the dataset repository
   */
  public HarvestParameterService(HarvestingParameterRepository harvestingParameterRepository,
      DatasetRepository datasetRepository) {
    this.harvestingParameterRepository = harvestingParameterRepository;
    this.datasetRepository = datasetRepository;
  }

  @Transactional
  public HarvestParametersEntity createDatasetHarvestParameters(String datasetId, HarvestParametersDTO harvestParametersDto) {
    requireNonNull(datasetId, "Dataset name must not be null");
    requireNonNull(harvestParametersDto, "Type of harvesting must not be null");
    try {
      DatasetEntity datasetEntity = datasetRepository.findById(Integer.parseInt(datasetId)).orElseThrow();
      HarvestParametersEntity harvestParametersEntity = HarvestParametersConverter.convertToHarvestParametersEntity(datasetEntity,
          harvestParametersDto);
      return harvestingParameterRepository.save(harvestParametersEntity);
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error saving harvesting parameters for dataset id: [%s]. ", datasetId), e);
    }

  }

  public Optional<HarvestParametersEntity> getDatasetHarvestingParameters(String datasetId) {
    return harvestingParameterRepository.findByDatasetEntity_DatasetId(Integer.parseInt(datasetId));
  }

  public Optional<HarvestParametersEntity> getHarvestingParametersById(UUID uuid) {
    return harvestingParameterRepository.findById(uuid);
  }

  @Transactional
  public void remove(String datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");
    try {
      harvestingParameterRepository.deleteByDatasetIdDatasetId(Integer.parseInt(datasetId));
    } catch (RuntimeException e) {
      throw new ServiceException(
          format("Error removing records for dataset id: [%s]. ", datasetId), e);
    }
  }
}
