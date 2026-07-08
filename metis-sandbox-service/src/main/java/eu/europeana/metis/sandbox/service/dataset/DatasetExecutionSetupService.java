package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.batch.dto.ExceptionInfoDTO;
import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.common.WorkflowType;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.DatasetMetadata;
import eu.europeana.metis.sandbox.dto.ExecutionMetadata;
import eu.europeana.metis.sandbox.dto.InputMetadata;
import eu.europeana.metis.sandbox.dto.harvest.AbstractHarvestParametersDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.DatasetError;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.XsltType;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class responsible for preparing, creating, and managing dataset execution setups.
 */
@AllArgsConstructor
@Service
public class DatasetExecutionSetupService {

  private final DatasetRepository datasetRepository;
  private final HarvestParameterService harvestParameterService;
  private final TransformXsltRepository transformXsltRepository;

  /**
   * Prepares the execution of a harvest operation for the specified dataset and harvest parameters.
   *
   * @param datasetId the dataset identifier
   * @param abstractHarvestParametersDTO the harvest parameters required to configure the execution
   * @return an instance of ExecutionMetadata containing metadata about the prepared execution
   */
  @Transactional
  public ExecutionMetadata prepareHarvestExecution(String datasetId, AbstractHarvestParametersDTO abstractHarvestParametersDTO) {
    return prepareExecution(WorkflowType.SINGLE, datasetId, null, null, abstractHarvestParametersDTO, null);
  }

  /**
   * Prepares an intermediate execution of a workflow based on the provided dataset identifier, source execution identifier, and
   * possibly an XSLT file.
   *
   * @param datasetId the dataset identifier
   * @param sourceExecutionId the source execution identifier to be used as a reference.
   * @return an instance of ExecutionMetadata containing metadata about the prepared execution
   */
  @Transactional
  public ExecutionMetadata prepareIntermediateExecution(String datasetId, String sourceExecutionId) {
    return prepareExecution(WorkflowType.SINGLE, datasetId, null, null, null, sourceExecutionId);
  }

  @Transactional
  public ExecutionMetadata prepareTransformExecution(String datasetId, String sourceExecutionId, String xslt, XsltType xsltType) {
    return prepareExecution(WorkflowType.SINGLE, datasetId, xslt, xsltType, null, sourceExecutionId);
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
   */
  @Transactional
  public ExecutionMetadata prepareDatasetAndExecution(
      WorkflowType workflowType,
      DatasetMetadataRequest datasetMetadataRequest,
      String userId,
      String xsltFile,
      AbstractHarvestParametersDTO abstractHarvestParametersDTO) {
    String datasetId = createDataset(datasetMetadataRequest, workflowType, userId);
    return prepareExecution(workflowType, datasetId, xsltFile, XsltType.INTERNAL, abstractHarvestParametersDTO, null);
  }

  private ExecutionMetadata prepareExecution(
      WorkflowType workflowType,
      String datasetId,
      String xslt,
      XsltType xsltType,
      AbstractHarvestParametersDTO abstractHarvestParametersDTO,
      String sourceExecutionId) {
    DatasetEntity datasetEntity = datasetRepository.findById(Integer.valueOf(datasetId)).orElseThrow();

    TransformXsltEntity transformXsltEntity = handleXslt(datasetId, xslt, xsltType);

    InputMetadata inputMetadata =
        buildInputMetadata(datasetId, abstractHarvestParametersDTO, sourceExecutionId, transformXsltEntity);
    DatasetMetadata datasetMetadata = DatasetMetadata.builder()
                                                     .datasetId(datasetId)
                                                     .datasetName(datasetEntity.getDatasetName())
                                                     .country(datasetEntity.getCountry())
                                                     .language(datasetEntity.getLanguage())
                                                     .workflowType(workflowType).build();
    return ExecutionMetadata.builder().datasetMetadata(datasetMetadata).inputMetadata(inputMetadata).build();
  }

  private @NotNull InputMetadata buildInputMetadata(String datasetId, AbstractHarvestParametersDTO abstractHarvestParametersDTO,
      String sourceExecutionId, TransformXsltEntity transformXsltEntity) {
    InputMetadata inputMetadata;
    if (abstractHarvestParametersDTO == null) {
      inputMetadata = new InputMetadata(sourceExecutionId, new InputMetadata(null, transformXsltEntity));
    } else {
      HarvestParametersEntity harvestParametersEntity = harvestParameterService.createDatasetHarvestParameters(datasetId,
          abstractHarvestParametersDTO);
      inputMetadata = new InputMetadata(harvestParametersEntity, transformXsltEntity);
    }
    return inputMetadata;
  }

  private @Nullable TransformXsltEntity handleXslt(String datasetId, String xslt, XsltType xsltType) {
    return (xslt != null) ? saveXslt(datasetId, xslt, xsltType) : null;
  }

  /**
   * Updates the record limit exceeded status for the specified dataset.
   *
   * @param datasetId the ID of the dataset to update
   */
  public void updateRecordLimitExceeded(int datasetId) {
    datasetRepository.updateRecordLimitExceeded(datasetId);
  }

  /**
   * Creates a new dataset based on the provided metadata, workflow type, and user identifier.
   *
   * @param datasetMetadataRequest the metadata information required for creating the dataset
   * @param workflowType the workflow type associated with the dataset
   * @param userId the identifier of the user initiating the dataset creation
   * @return the unique identifier of the newly created dataset as a string
   * @throws ServiceException if there is an error during the dataset creation process
   */
  public String createDataset(DatasetMetadataRequest datasetMetadataRequest, WorkflowType workflowType, String userId) {
    DatasetEntity datasetEntity = new DatasetEntity(datasetMetadataRequest.getDatasetName(), workflowType,
        datasetMetadataRequest.getLanguage(), datasetMetadataRequest.getCountry(), userId);

    try {
      return String.valueOf(datasetRepository.save(datasetEntity).getDatasetId());
    } catch (RuntimeException e) {
      throw new ServiceException(format("Failed to create dataset [%s]", datasetMetadataRequest.getDatasetName()), e);
    }
  }

  private @NotNull TransformXsltEntity saveXslt(String datasetId, String xsltFile, XsltType xsltType) {
    TransformXsltEntity entity;
    if (xsltType == XsltType.DEFAULT) {
      throw new ServiceException("DEFAULT XSLT is not valid here");
    } else {
      entity = transformXsltRepository
          .findByDatasetIdAndType(datasetId, xsltType)
          .orElseGet(() -> new TransformXsltEntity(datasetId, xsltType, xsltFile));
    }
    entity.setTransformXslt(xsltFile);
    return transformXsltRepository.save(entity);
  }

  /**
   * Updates the dataset with an error by associating an exception's details with the dataset.
   *
   * @param datasetId the ID of the dataset to update with the error
   * @param exception the exception containing the error details to associate with the dataset
   */
  @Transactional
  public void updateDatasetWithError(String datasetId, Exception exception) {
    DatasetEntity dataset = datasetRepository.findById(Integer.valueOf(datasetId))
                                             .orElseThrow(() -> new RuntimeException("Dataset not found"));

    ExceptionInfoDTO exceptionInfoDTO = ExceptionInfoDTO.from(exception);

    DatasetError datasetError = new DatasetError();
    datasetError.setMessage(exceptionInfoDTO.getMessage());
    datasetError.setException(exceptionInfoDTO.getStackTrace());
    datasetError.setDatasetEntity(dataset);

    dataset.getDatasetErrors().add(datasetError);

    datasetRepository.save(dataset);
  }
}
