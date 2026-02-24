package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.batch.dto.ExceptionInfoDTO;
import eu.europeana.metis.sandbox.dto.DatasetMetadata;
import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.dto.ExecutionMetadata;
import eu.europeana.metis.sandbox.dto.InputMetadata;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.harvest.AbstractHarvestParametersDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.DatasetError;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.common.WorkflowType;
import eu.europeana.metis.sandbox.entity.XsltType;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service class responsible for preparing, creating, and managing dataset execution setups.
 */
@AllArgsConstructor
@Service
public class DatasetExecutionSetupService {

  private final DatasetRepository datasetRepository;
  private final HarvestParameterService harvestParameterService;
  private final TransformXsltRepository transformXsltRepository;

  @Transactional
  public ExecutionMetadata prepareDatasetExecutionHarvest(String datasetId,
      AbstractHarvestParametersDTO abstractHarvestParametersDTO) {
    //We assume dataset was already created with another call
    DatasetEntity datasetEntity = datasetRepository.findById(Integer.valueOf(datasetId)).orElseThrow();

    HarvestParametersEntity harvestParametersEntity =
        harvestParameterService.createDatasetHarvestParameters(datasetId, abstractHarvestParametersDTO);
    InputMetadata inputMetadata = new InputMetadata(harvestParametersEntity, null);

    DatasetMetadata datasetMetadata = DatasetMetadata.builder()
                                                     .datasetId(datasetId)
                                                     .datasetName(datasetEntity.getDatasetName())
                                                     .country(datasetEntity.getCountry())
                                                     .language(datasetEntity.getLanguage())
                                                     .workflowType(WorkflowType.SINGLE).build();

    return ExecutionMetadata.builder().datasetMetadata(datasetMetadata).inputMetadata(inputMetadata).build();
  }

  @Transactional
  public ExecutionMetadata prepareDatasetExecution(String datasetId, String sourceExecutionId, MultipartFile xsltFile) throws IOException {
    //We assume dataset was already created with another call
    DatasetEntity datasetEntity = datasetRepository.findById(Integer.valueOf(datasetId)).orElseThrow();
    TransformXsltEntity transformXsltEntity = (xsltFile != null)
        ? saveXslt(xsltFile, datasetId)
        : null;
    InputMetadata inputMetadata = new InputMetadata(sourceExecutionId, new InputMetadata(null, transformXsltEntity));
    DatasetMetadata datasetMetadata = DatasetMetadata.builder()
                                                     .datasetId(datasetId)
                                                     .datasetName(datasetEntity.getDatasetName())
                                                     .country(datasetEntity.getCountry())
                                                     .language(datasetEntity.getLanguage())
                                                     .workflowType(WorkflowType.SINGLE).build();

    return ExecutionMetadata.builder().datasetMetadata(datasetMetadata).inputMetadata(inputMetadata).build();

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

    DatasetMetadata datasetMetadata = DatasetMetadata.builder()
                                                     .datasetId(datasetId)
                                                     .datasetName(datasetMetadataRequest.getDatasetName())
                                                     .country(datasetMetadataRequest.getCountry())
                                                     .language(datasetMetadataRequest.getLanguage())
                                                     .workflowType(workflowType).build();

    return ExecutionMetadata.builder().datasetMetadata(datasetMetadata).inputMetadata(inputMetadata).build();
  }

  /**
   * Updates the record limit exceeded status for the specified dataset.
   *
   * @param datasetId the ID of the dataset to update
   */
  public void updateRecordLimitExceeded(int datasetId) {
    datasetRepository.updateRecordLimitExceeded(datasetId);
  }

  public String createDataset(DatasetMetadataRequest datasetMetadataRequest, WorkflowType workflowType, String userId) {
    DatasetEntity datasetEntity = new DatasetEntity(datasetMetadataRequest.getDatasetName(), workflowType,
        datasetMetadataRequest.getLanguage(), datasetMetadataRequest.getCountry(), userId);

    try {
      return String.valueOf(datasetRepository.save(datasetEntity).getDatasetId());
    } catch (RuntimeException e) {
      throw new ServiceException(format("Failed to create dataset [%s]", datasetMetadataRequest.getDatasetName()), e);
    }
  }

  private @NotNull TransformXsltEntity saveXslt(MultipartFile xsltFile, String datasetId) throws IOException {
    TransformXsltEntity transformXsltEntity = new TransformXsltEntity(datasetId, XsltType.EXTERNAL,
        new String(xsltFile.getBytes(), StandardCharsets.UTF_8));
    transformXsltRepository.save(transformXsltEntity);
    return transformXsltEntity;
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
