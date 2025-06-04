package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.harvest.FileHarvestDTO;
import eu.europeana.metis.sandbox.dto.harvest.HarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.harvest.HttpHarvestDTO;
import eu.europeana.metis.sandbox.dto.harvest.OaiHarvestDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.harvest.FileHarvestParameters;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.HttpHarvestParameters;
import eu.europeana.metis.sandbox.entity.harvest.OaiHarvestParameters;
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
public class HarvestingParameterService {

  private final HarvestingParameterRepository harvestingParameterRepository;
  private final DatasetRepository datasetRepository;

  /**
   * Constructor
   *
   * @param harvestingParameterRepository The repository that connected to the table harvesting parameters
   * @param datasetRepository The repository that connects to the dataset repository
   */
  public HarvestingParameterService(HarvestingParameterRepository harvestingParameterRepository,
      DatasetRepository datasetRepository) {
    this.harvestingParameterRepository = harvestingParameterRepository;
    this.datasetRepository = datasetRepository;
  }

  @Transactional
  public HarvestParametersEntity createDatasetHarvestParameters(String datasetId, HarvestParametersDTO harvestParametersDto) {
    requireNonNull(datasetId, "Dataset name must not be null");
    requireNonNull(harvestParametersDto, "Type of harvesting must not be null");
    try {
      return harvestingParameterRepository.save(createEntityToSave(datasetId, harvestParametersDto));
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error saving harvesting parameters for dataset id: [%s]. ", datasetId), e);
    }

  }

  public HarvestParametersEntity getDatasetHarvestingParameters(String datasetId) {
    return harvestingParameterRepository.getHarvestingParametersEntitiesByDatasetId_DatasetId(Integer.parseInt(datasetId));
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

  private HarvestParametersEntity createEntityToSave(String datasetId, HarvestParametersDTO harvestParametersDto) {

    DatasetEntity datasetEntity = datasetRepository.findById(Integer.parseInt(datasetId)).orElseThrow();

    return switch (harvestParametersDto) {
      case OaiHarvestDTO oaiHarvestDTO -> {
        OaiHarvestParameters oaiHarvestParameters = new OaiHarvestParameters();
        oaiHarvestParameters.setDatasetId(datasetEntity);
        oaiHarvestParameters.setUrl(oaiHarvestDTO.getUrl());
        oaiHarvestParameters.setSetSpec(oaiHarvestDTO.getSetSpec());
        oaiHarvestParameters.setMetadataFormat(oaiHarvestDTO.getMetadataFormat());
        yield oaiHarvestParameters;
      }
      case HttpHarvestDTO httpHarvestDTO -> {
        HttpHarvestParameters httpHarvestParameters = new HttpHarvestParameters();
        httpHarvestParameters.setDatasetId(datasetEntity);
        httpHarvestParameters.setUrl(httpHarvestDTO.getUrl());
        httpHarvestParameters.setFileName(httpHarvestDTO.getFileName());
        httpHarvestParameters.setFileType(httpHarvestDTO.getFileType());
        httpHarvestParameters.setFileContent(httpHarvestDTO.getFileContent());
        yield httpHarvestParameters;
      }
      case FileHarvestDTO fileHarvestDTO -> {
        FileHarvestParameters fileHarvestParameters = new FileHarvestParameters();
        fileHarvestParameters.setDatasetId(datasetEntity);
        fileHarvestParameters.setFileName(fileHarvestDTO.getFileName());
        fileHarvestParameters.setFileType(fileHarvestDTO.getFileType());
        fileHarvestParameters.setFileContent(fileHarvestDTO.getFileContent());
        yield fileHarvestParameters;
      }
      default -> throw new IllegalArgumentException("Unsupported harvest parameters type: " + harvestParametersDto.getClass());
    };
  }
}
