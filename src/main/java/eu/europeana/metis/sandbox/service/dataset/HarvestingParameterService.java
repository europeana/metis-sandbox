package eu.europeana.metis.sandbox.service.dataset;

import static eu.europeana.metis.sandbox.common.HarvestProtocol.FILE;
import static eu.europeana.metis.sandbox.common.HarvestProtocol.HTTP;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.FileHarvestDTO;
import eu.europeana.metis.sandbox.dto.HarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.HttpHarvestDTO;
import eu.europeana.metis.sandbox.dto.OAIPmhHarvestDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.HarvestParametersEntity;
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

    private HarvestParametersEntity createEntityToSave(String datasetId, HarvestParametersDTO harvestParametersDto){

        DatasetEntity datasetEntity = datasetRepository.findById(Integer.parseInt(datasetId)).orElseThrow();

      return switch (harvestParametersDto.getHarvestProtocol()) {
        case FILE -> {
          FileHarvestDTO fileHarvestingDto = (FileHarvestDTO) harvestParametersDto;
          HarvestParametersEntity harvestParametersEntity = new HarvestParametersEntity();
          harvestParametersEntity.setDatasetId(datasetEntity);
          harvestParametersEntity.setHarvestProtocol(FILE);
          harvestParametersEntity.setFileName(fileHarvestingDto.getFileName());
          harvestParametersEntity.setFileType(fileHarvestingDto.getFileType());
          harvestParametersEntity.setFileContent(fileHarvestingDto.getFileData());
          yield harvestParametersEntity;
        }
        case HTTP -> {
          HttpHarvestDTO httpHarvestingDto = (HttpHarvestDTO) harvestParametersDto;
          HarvestParametersEntity harvestParametersEntity = new HarvestParametersEntity();
          harvestParametersEntity.setDatasetId(datasetEntity);
          harvestParametersEntity.setHarvestProtocol(HTTP);
          harvestParametersEntity.setUrl(httpHarvestingDto.getUrl());
          harvestParametersEntity.setFileType(httpHarvestingDto.getFileType());
          harvestParametersEntity.setFileContent(httpHarvestingDto.getFileData());
          yield harvestParametersEntity;
        }
        case OAI_PMH -> {
          OAIPmhHarvestDTO oaiPmhHarvestingDto = (OAIPmhHarvestDTO) harvestParametersDto;
          HarvestParametersEntity harvestParametersEntity = new HarvestParametersEntity();
          harvestParametersEntity.setDatasetId(datasetEntity);
          harvestParametersEntity.setHarvestProtocol(HTTP);
          harvestParametersEntity.setUrl(oaiPmhHarvestingDto.getUrl());
          harvestParametersEntity.setSetSpec(oaiPmhHarvestingDto.getSetSpec());
          harvestParametersEntity.setMetadataFormat(oaiPmhHarvestingDto.getMetadataFormat());
          yield harvestParametersEntity;
        }
      };
    }
}
