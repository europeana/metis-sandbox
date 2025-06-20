package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.DatasetMetadata;
import eu.europeana.metis.sandbox.common.ExecutionMetadata;
import eu.europeana.metis.sandbox.common.InputMetadata;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.harvest.HarvestParametersDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import eu.europeana.metis.sandbox.entity.XsltType;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.tika.utils.StringUtils;
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
   * @param datasetName the name of the dataset to be created
   * @param country the country associated with the dataset
   * @param language the language of the dataset
   * @param userId the ID of the user initiating the operation
   * @param xsltFile the optional XSLT file for transformation (nullable)
   * @param harvestParametersDTO the DTO containing the harvesting parameters
   * @return the execution metadata containing dataset and input metadata
   * @throws IOException if an error occurs while processing the XSLT file
   */
  @Transactional
  public ExecutionMetadata prepareDatasetExecution(
      WorkflowType workflowType,
      String datasetName,
      Country country,
      Language language,
      String userId,
      MultipartFile xsltFile,
      HarvestParametersDTO harvestParametersDTO
  ) throws IOException {
    String datasetId = createDataset(datasetName, workflowType, language, country, userId);

    TransformXsltEntity transformXsltEntity = (xsltFile != null)
        ? saveXslt(xsltFile, datasetId)
        : null;

    HarvestParametersEntity harvestParametersEntity =
        harvestParameterService.createDatasetHarvestParameters(datasetId, harvestParametersDTO);
    InputMetadata inputMetadata = new InputMetadata(harvestParametersEntity, transformXsltEntity);

    DatasetMetadata datasetMetadata = new DatasetMetadata(datasetId, datasetName, country, language, workflowType);
    return ExecutionMetadata.builder().datasetMetadata(datasetMetadata).inputMetadata(inputMetadata).build();
  }

  private String createDataset(String datasetName, WorkflowType workflowType, Language language, Country country, String userId) {
    requireNonNull(datasetName, "Dataset name must not be null");
    if(StringUtils.isBlank(datasetName)) {
      throw new IllegalArgumentException("Dataset name must not be empty");
    }
    requireNonNull(country, "Country must not be null");
    requireNonNull(language, "Language must not be null");

    DatasetEntity datasetEntity = new DatasetEntity(datasetName, workflowType, language, country, userId);

    try {
      return String.valueOf(datasetRepository.save(datasetEntity).getDatasetId());
    } catch (RuntimeException e) {
      throw new ServiceException(format("Failed to create dataset [%s]", datasetName), e);
    }
  }

  private @NotNull TransformXsltEntity saveXslt(MultipartFile xsltFile, String datasetId) throws IOException {
    TransformXsltEntity transformXsltEntity = new TransformXsltEntity(datasetId, XsltType.EXTERNAL,
        new String(xsltFile.getBytes(), StandardCharsets.UTF_8));
    transformXsltRepository.save(transformXsltEntity);
    return transformXsltEntity;
  }
}
