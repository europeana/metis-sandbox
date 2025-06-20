package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.common.DatasetMetadata;
import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.common.ExecutionMetadata;
import eu.europeana.metis.sandbox.common.InputMetadata;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.harvest.AbstractHarvestParametersDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import eu.europeana.metis.sandbox.entity.XsltType;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service class responsible for preparing, creating, and managing dataset execution setups.
 */
@Service
public class DatasetExecutionSetupService {

  private final DatasetRepository datasetRepository;
  private final HarvestParameterService harvestParameterService;
  private final TransformXsltRepository transformXsltRepository;

  /**
   * Constructor.
   *
   * @param datasetRepository the repository for dataset entity operations
   * @param harvestParameterService the service for managing harvest parameters
   * @param transformXsltRepository the repository for saving XSLT transformations
   */
  public DatasetExecutionSetupService(DatasetRepository datasetRepository, HarvestParameterService harvestParameterService,
      TransformXsltRepository transformXsltRepository) {
    this.datasetRepository = datasetRepository;
    this.harvestParameterService = harvestParameterService;
    this.transformXsltRepository = transformXsltRepository;
  }

  /**
   * Prepares the execution metadata for a dataset based on provided parameters.
   *
   * <p>This includes creating a dataset, saving an optional XSLT transformation file and generating harvest parameters for the
   * processing task.
   * <p>Returns an {@link ExecutionMetadata} object containing dataset and input metadata required for the processing task.
   *
   * @param workflowType the type of workflow to be executed
   * @param datasetMetadataRequest the name of the dataset to be created
   * @param userId the ID of the user initiating the operation
   * @param xsltFile the optional XSLT file for transformation (nullable)
   * @param abstractHarvestParametersDTO the DTO containing the harvesting parameters
   * @return the execution metadata containing dataset and input metadata
   * @throws IOException if an error occurs while processing the XSLT file
   */
  @Transactional
  public ExecutionMetadata prepareDatasetExecution(
      WorkflowType workflowType,
      DatasetMetadataRequest datasetMetadataRequest,
      String userId,
      MultipartFile xsltFile,
      AbstractHarvestParametersDTO abstractHarvestParametersDTO
  ) throws IOException {
    String datasetId = createDataset(datasetMetadataRequest, workflowType, userId);

    TransformXsltEntity transformXsltEntity = (xsltFile != null)
        ? saveXslt(xsltFile, datasetId)
        : null;

    HarvestParametersEntity harvestParametersEntity =
        harvestParameterService.createDatasetHarvestParameters(datasetId, abstractHarvestParametersDTO);
    InputMetadata inputMetadata = new InputMetadata(harvestParametersEntity, transformXsltEntity);

    DatasetMetadata datasetMetadata = new DatasetMetadata(datasetId, datasetMetadataRequest.datasetName(),
        datasetMetadataRequest.country(), datasetMetadataRequest.language(), workflowType);
    return ExecutionMetadata.builder().datasetMetadata(datasetMetadata).inputMetadata(inputMetadata).build();
  }

  private String createDataset(DatasetMetadataRequest datasetMetadataRequest, WorkflowType workflowType, String userId) {
    DatasetEntity datasetEntity = new DatasetEntity(datasetMetadataRequest.datasetName(), workflowType,
        datasetMetadataRequest.language(), datasetMetadataRequest.country(), userId);

    try {
      return String.valueOf(datasetRepository.save(datasetEntity).getDatasetId());
    } catch (RuntimeException e) {
      throw new ServiceException(format("Failed to create dataset [%s]", datasetMetadataRequest.datasetName()), e);
    }
  }

  private @NotNull TransformXsltEntity saveXslt(MultipartFile xsltFile, String datasetId) throws IOException {
    TransformXsltEntity transformXsltEntity = new TransformXsltEntity(datasetId, XsltType.EXTERNAL,
        new String(xsltFile.getBytes(), StandardCharsets.UTF_8));
    transformXsltRepository.save(transformXsltEntity);
    return transformXsltEntity;
  }
}
