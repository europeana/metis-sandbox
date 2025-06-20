package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.tika.utils.StringUtils.isBlank;

import eu.europeana.metis.sandbox.common.HarvestParametersConverter;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.harvest.AbstractHarvestParametersDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.HarvestParametersRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class responsible for managing harvest parameters related to datasets.
 */
@Service
public class HarvestParameterService {

  private final HarvestParametersRepository harvestParametersRepository;
  private final DatasetRepository datasetRepository;

  /**
   * Constructor.
   *
   * @param harvestParametersRepository The repository that connected to the table harvesting parameters
   * @param datasetRepository The repository that connects to the dataset repository
   */
  public HarvestParameterService(HarvestParametersRepository harvestParametersRepository,
      DatasetRepository datasetRepository) {
    this.harvestParametersRepository = harvestParametersRepository;
    this.datasetRepository = datasetRepository;
  }

  /**
   * Creates and saves harvest parameters for a given dataset.
   *
   * <p>Validates that the dataset exists and converts the provided DTO into
   * an entity before persisting it in the repository.
   *
   * @param datasetId the identifier of the dataset the parameters are associated with
   * @param abstractHarvestParametersDTO the data transfer object containing the harvest parameters
   * @return the saved HarvestParametersEntity
   * @throws ServiceException in case a {@link RuntimeException} occurred
   */
  @Transactional
  public HarvestParametersEntity createDatasetHarvestParameters(String datasetId, AbstractHarvestParametersDTO abstractHarvestParametersDTO) {
    requireNonNull(datasetId, "Dataset id must not be null");
    if (isBlank(datasetId)) {
      throw new IllegalArgumentException("Dataset id must not be empty");
    }
    requireNonNull(abstractHarvestParametersDTO, "Type of harvesting must not be null");
    try {
      DatasetEntity datasetEntity = datasetRepository.findById(Integer.parseInt(datasetId)).orElseThrow();
      HarvestParametersEntity harvestParametersEntity = HarvestParametersConverter.convertToHarvestParametersEntity(datasetEntity,
          abstractHarvestParametersDTO);
      return harvestParametersRepository.save(harvestParametersEntity);
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error saving harvesting parameters for dataset id: [%s]. ", datasetId), e);
    }
  }

  /**
   * Retrieves the harvesting parameters associated with a specific dataset identifier.
   *
   * @param datasetId the identifier of the dataset for which harvesting parameters are to be retrieved
   * @return an Optional containing the HarvestParametersEntity if a match is found, or empty if not
   */
  public Optional<HarvestParametersEntity> getDatasetHarvestingParameters(String datasetId) {
    return harvestParametersRepository.findByDatasetEntity_DatasetId(Integer.parseInt(datasetId));
  }

  /**
   * Retrieves the harvest parameters associated with the given UUID.
   *
   * @param uuid the unique identifier of the harvest parameters to be retrieved
   * @return an Optional containing the HarvestParametersEntity if found, or empty if not
   */
  public Optional<HarvestParametersEntity> getHarvestingParametersById(UUID uuid) {
    return harvestParametersRepository.findById(uuid);
  }

  /**
   * Removes harvesting parameter records associated with a specific dataset identifier.
   *
   * @param datasetId the identifier of the dataset whose records are to be removed
   */
  @Transactional
  public void remove(String datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");
    if (isBlank(datasetId)) {
      throw new IllegalArgumentException("Dataset id must not be empty");
    }
    try {
      harvestParametersRepository.deleteByDatasetIdDatasetId(Integer.parseInt(datasetId));
    } catch (RuntimeException e) {
      throw new ServiceException(
          format("Error removing records for dataset id: [%s]. ", datasetId), e);
    }
  }
}
