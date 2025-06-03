package eu.europeana.metis.sandbox.service.dataset;

import static eu.europeana.metis.sandbox.common.HarvestProtocol.FILE;
import static eu.europeana.metis.sandbox.common.HarvestProtocol.HTTP;
import static eu.europeana.metis.sandbox.common.HarvestProtocol.OAI_PMH;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.FileHarvestingDTO;
import eu.europeana.metis.sandbox.dto.HarvestingParametersDTO;
import eu.europeana.metis.sandbox.dto.HttpHarvestingDTO;
import eu.europeana.metis.sandbox.dto.OAIPmhHarvestingDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.HarvestingParameterEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.HarvestingParameterRepository;
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
    public void createDatasetHarvestingParameters(String datasetId, HarvestingParametersDTO harvestingParametersDto) {
        requireNonNull(datasetId, "Dataset name must not be null");
        requireNonNull(harvestingParametersDto, "Type of harvesting must not be null");
        try {
            harvestingParameterRepository.save(createEntityToSave(datasetId, harvestingParametersDto));
        } catch (RuntimeException e) {
            throw new ServiceException(format("Error saving harvesting parameters for dataset id: [%s]. ", datasetId), e);
        }

    }

    public HarvestingParameterEntity getDatasetHarvestingParameters(String datasetId) {
        return harvestingParameterRepository.getHarvestingParametersEntitiesByDatasetId_DatasetId(Integer.parseInt(datasetId));
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

    private HarvestingParameterEntity createEntityToSave(String datasetId, HarvestingParametersDTO harvestingParametersDto){

        DatasetEntity datasetEntity = datasetRepository.findById(Integer.parseInt(datasetId)).orElseThrow();

      return switch (harvestingParametersDto.getHarvestProtocol()) {
        case FILE -> {
          FileHarvestingDTO fileHarvestingDto = (FileHarvestingDTO) harvestingParametersDto;
          yield new HarvestingParameterEntity(datasetEntity, FILE, fileHarvestingDto.getFileName(),
              fileHarvestingDto.getFileType(), null, null, null);
        }
        case HTTP -> {
          HttpHarvestingDTO httpHarvestingDto = (HttpHarvestingDTO) harvestingParametersDto;
          yield new HarvestingParameterEntity(datasetEntity, HTTP, null, null,
              httpHarvestingDto.getUrl(), null, null);
        }
        case OAI_PMH -> {
          OAIPmhHarvestingDTO oaiPmhHarvestingDto = (OAIPmhHarvestingDTO) harvestingParametersDto;
          yield new HarvestingParameterEntity(datasetEntity, OAI_PMH, null, null,
              oaiPmhHarvestingDto.getUrl(), oaiPmhHarvestingDto.getSetSpec(), oaiPmhHarvestingDto.getMetadataFormat());
        }
      };
    }
}
