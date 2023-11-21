package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.FileHarvestingDto;
import eu.europeana.metis.sandbox.dto.HarvestingParametricDto;
import eu.europeana.metis.sandbox.dto.HttpHarvestingDto;
import eu.europeana.metis.sandbox.dto.OAIPmhHarvestingDto;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.HarvestingParameterEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.HarvestingParameterRepository;
import org.hibernate.procedure.NoSuchParameterException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static eu.europeana.metis.sandbox.common.HarvestProtocol.*;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Service class to access harvesting parameters table in the database
 */
@Service
public class HarvestingParameterServiceImpl implements HarvestingParameterService {

    private final HarvestingParameterRepository harvestingParameterRepository;
    private final DatasetRepository datasetRepository;

    /**
     * Constructor
     * @param harvestingParameterRepository The repository that connected to the table harvesting parameters
     * @param datasetRepository The repository that connects to the dataset repository
     */
    public HarvestingParameterServiceImpl(HarvestingParameterRepository harvestingParameterRepository,
                                          DatasetRepository datasetRepository) {
        this.harvestingParameterRepository = harvestingParameterRepository;
        this.datasetRepository = datasetRepository;
    }


    @Override
    @Transactional
    public void createDatasetHarvestingParameters(String datasetId, HarvestingParametricDto harvestingParametricDto) {
        requireNonNull(datasetId, "Dataset name must not be null");
        requireNonNull(harvestingParametricDto, "Type of harvesting must not be null");
        try {
            harvestingParameterRepository.save(createEntityToSave(datasetId, harvestingParametricDto));
        } catch (RuntimeException e) {
            throw new ServiceException(format("Error saving harvesting parameters for dataset id: [%s]. ", datasetId), e);
        }

    }

    @Override
    public HarvestingParameterEntity getDatasetHarvestingParameters(String datasetId) {
        return harvestingParameterRepository.getHarvestingParametersEntitiesByDatasetId_DatasetId(Integer.parseInt(datasetId));
    }

    @Override
    @Transactional
    public void remove(String datasetId) {
        requireNonNull(datasetId, "Dataset id must not be null");
        try {
            harvestingParameterRepository.deleteByDatasetId_DatasetId(Integer.parseInt(datasetId));
        } catch (RuntimeException e) {
            throw new ServiceException(
                    format("Error removing records for dataset id: [%s]. ", datasetId), e);
        }
    }

    private HarvestingParameterEntity createEntityToSave(String datasetId, HarvestingParametricDto harvestingParametricDto){

        DatasetEntity datasetEntity = datasetRepository.findById(Integer.parseInt(datasetId)).orElseThrow();

        switch(harvestingParametricDto.getHarvestProtocol()){
            case FILE:
                FileHarvestingDto fileHarvestingDto = (FileHarvestingDto) harvestingParametricDto;
                return new HarvestingParameterEntity(datasetEntity, FILE, fileHarvestingDto.getFileName(),
                        fileHarvestingDto.getFileType(), null, null, null);

            case HTTP:
                HttpHarvestingDto httpHarvestingDto = (HttpHarvestingDto) harvestingParametricDto;
                return new HarvestingParameterEntity(datasetEntity, HTTP, null, null,
                        httpHarvestingDto.getUrl(), null, null);

            case OAI_PMH:
                OAIPmhHarvestingDto oaiPmhHarvestingDto = (OAIPmhHarvestingDto) harvestingParametricDto;
                return new HarvestingParameterEntity(datasetEntity, OAI_PMH, null, null,
                        oaiPmhHarvestingDto.getUrl(), oaiPmhHarvestingDto.getSetSpec(), oaiPmhHarvestingDto.getMetadataFormat());

            default:
                throw new NoSuchParameterException("No such value for harvest protocol exists");

        }


    }
}
