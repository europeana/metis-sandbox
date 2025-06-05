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
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DatasetExecutionSetupService {

  private final DatasetRepository datasetRepository;
  private final HarvestingParameterService harvestingParameterService;
  private final TransformXsltRepository transformXsltRepository;

  public DatasetExecutionSetupService(DatasetRepository datasetRepository, HarvestingParameterService harvestingParameterService,
      TransformXsltRepository transformXsltRepository) {
    this.datasetRepository = datasetRepository;
    this.harvestingParameterService = harvestingParameterService;
    this.transformXsltRepository = transformXsltRepository;
  }

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
    String datasetId = createDataset(workflowType, datasetName, userId, country, language);

    TransformXsltEntity transformXsltEntity = (xsltFile != null)
        ? saveXslt(xsltFile, datasetId)
        : null;

    HarvestParametersEntity harvestParametersEntity =
        harvestingParameterService.createDatasetHarvestParameters(datasetId, harvestParametersDTO);
    InputMetadata inputMetadata = new InputMetadata(harvestParametersEntity, transformXsltEntity);

    DatasetMetadata datasetMetadata = new DatasetMetadata(datasetId, datasetName, country, language, workflowType);
    return ExecutionMetadata.builder().datasetMetadata(datasetMetadata).inputMetadata(inputMetadata).build();
  }

  public String createDataset(WorkflowType workflowType, String name, String userId, Country country,
      Language language) {
    requireNonNull(name, "Dataset name is required");
    requireNonNull(country, "Country is required");
    requireNonNull(language, "Language is required");

    DatasetEntity datasetEntity = new DatasetEntity(name, workflowType, language, country, userId, null, false);

    try {
      return String.valueOf(datasetRepository.save(datasetEntity).getDatasetId());
    } catch (RuntimeException e) {
      throw new ServiceException(format("Failed to create dataset [%s]", name), e);
    }
  }

  private @NotNull TransformXsltEntity saveXslt(MultipartFile xsltFile, String datasetId) throws IOException {
    TransformXsltEntity transformXsltEntity = new TransformXsltEntity(datasetId, XsltType.EXTERNAL, new String(xsltFile.getBytes(), StandardCharsets.UTF_8));
        transformXsltRepository.save(transformXsltEntity);
    return transformXsltEntity;
  }
}
