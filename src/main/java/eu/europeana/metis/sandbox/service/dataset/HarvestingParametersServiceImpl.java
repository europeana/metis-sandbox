package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.Protocol;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.entity.HarvestingParametersEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.HarvestingParametersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@Service
public class HarvestingParametersServiceImpl implements HarvestingParametersService{

    private final HarvestingParametersRepository harvestingParametersRepository;
    private final DatasetRepository datasetRepository;
    public HarvestingParametersServiceImpl(HarvestingParametersRepository harvestingParametersRepository,
                                           DatasetRepository datasetRepository) {
        this.harvestingParametersRepository = harvestingParametersRepository;
        this.datasetRepository = datasetRepository;
    }


    @Override
    @Transactional
    public HarvestingParametersEntity createDatasetHarvestingParameters(String datasetId, Protocol protocol,
                                                                        String fileName, String fileType, String url,
                                                                        String setSpec, String metadataFormat) {
        requireNonNull(datasetId, "Dataset name must not be null");
        requireNonNull(protocol, "Type of harvesting must not be null");
        try {
            return harvestingParametersRepository.save(new HarvestingParametersEntity(datasetRepository.findById(Integer.parseInt(datasetId)).orElseThrow(),
                    protocol, fileName, fileType, url, setSpec, metadataFormat));
        } catch (RuntimeException e) {
            throw new ServiceException(format("Error saving harvesting parameters for dataset id: [%s]. ", datasetId), e);
        }

    }

    @Override
    public HarvestingParametersEntity getDatasetHarvestingParameters(String datasetId) {
        return harvestingParametersRepository.getHarvestingParametersEntitiesByDatasetId_DatasetId(Integer.parseInt(datasetId));
    }

    @Override
    @Transactional
    public void remove(String datasetId) {
        requireNonNull(datasetId, "Dataset id must not be null");
        try {
            harvestingParametersRepository.deleteByDatasetId_DatasetId(Integer.parseInt(datasetId));
        } catch (RuntimeException e) {
            throw new ServiceException(
                    format("Error removing records for dataset id: [%s]. ", datasetId), e);
        }
    }
}
