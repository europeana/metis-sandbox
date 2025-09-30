package eu.europeana.metis.sandbox.service.debias;

import static java.util.Objects.requireNonNull;
import static org.apache.tika.utils.StringUtils.isBlank;

import eu.europeana.metis.debias.detect.model.response.Tag;
import eu.europeana.metis.debias.detect.model.response.ValueDetection;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.config.batch.DebiasJobConfig;
import eu.europeana.metis.sandbox.dto.debias.DeBiasReportDTO;
import eu.europeana.metis.sandbox.dto.debias.DeBiasStatusDTO;
import eu.europeana.metis.sandbox.dto.debias.DebiasState;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasDetailEntity;
import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasMainEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.debias.DatasetDeBiasRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasDetailRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasMainRepository;
import eu.europeana.metis.sandbox.service.debias.DeBiasProcessService.DeBiasReportRow;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class responsible for managing the debias state and operations.
 */
public class DeBiasStateService {

  private final DatasetDeBiasRepository datasetDeBiasRepository;
  private final RecordDeBiasMainRepository recordDeBiasMainRepository;
  private final RecordDeBiasDetailRepository recordDeBiasDetailRepository;
  private final DatasetRepository datasetRepository;
  private final ExecutionRecordRepository executionRecordRepository;

  /**
   * Constructor.
   *
   * @param datasetDeBiasRepository repository for managing dataset debias entities
   * @param datasetRepository repository for accessing and managing dataset entities
   * @param recordDeBiasMainRepository repository for managing main record debias entries
   * @param recordDeBiasDetailRepository repository for managing detail record debias entries
   * @param executionRecordRepository repository for managing execution record entities
   */
  public DeBiasStateService(
      DatasetDeBiasRepository datasetDeBiasRepository,
      DatasetRepository datasetRepository,
      RecordDeBiasMainRepository recordDeBiasMainRepository,
      RecordDeBiasDetailRepository recordDeBiasDetailRepository,
      ExecutionRecordRepository executionRecordRepository) {
    this.datasetDeBiasRepository = datasetDeBiasRepository;
    this.recordDeBiasMainRepository = recordDeBiasMainRepository;
    this.recordDeBiasDetailRepository = recordDeBiasDetailRepository;
    this.datasetRepository = datasetRepository;
    this.executionRecordRepository = executionRecordRepository;
  }

  /**
   * Generates a debiasing report for a specific dataset.
   *
   * @param datasetId the id of the dataset for which the debias report is to be generated
   * @return the debias report
   */
  public DeBiasReportDTO getDeBiasReport(String datasetId) {
    DeBiasStatusDTO deBiasStatusDto = getDeBiasStatus(datasetId);
    return new DeBiasReportDTO(Integer.valueOf(datasetId), deBiasStatusDto.getDebiasState(),
        deBiasStatusDto.getCreationDate(), deBiasStatusDto.getTotal(),
        deBiasStatusDto.getProcessed(), generateDebiasReportRows(datasetId));
  }

  /**
   * Retrieves the debiasing status of a specified dataset.
   *
   * <p>Determines the status of the dataset debiasing process based on total records to debias, total records already debiased.
   *
   * @param datasetId the id of the dataset for which the debiasing status is being retrieved
   * @return a {@code DeBiasStatusDTO} instance containing the debiasing status of the dataset
   */
  public DeBiasStatusDTO getDeBiasStatus(String datasetId) {
    DatasetDeBiasEntity datasetDeBiasEntity = datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(
        Integer.valueOf(datasetId));
    long totalRecordsToDebias = executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        FullBatchJobType.VALIDATE_INTERNAL.name());
    long totalRecordsDebiased = executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        DebiasJobConfig.BATCH_JOB.name());

    final DebiasState debiasState;
    if (totalRecordsToDebias > 0 && (totalRecordsToDebias == totalRecordsDebiased)) {
      debiasState = DebiasState.COMPLETED;
    } else if (totalRecordsToDebias >= 0 && totalRecordsDebiased == 0) {
      debiasState = DebiasState.READY;
    } else if (totalRecordsToDebias > 0 && totalRecordsDebiased > 0) {
      debiasState = DebiasState.PROCESSING;
    } else {
      debiasState = DebiasState.INVALID;
    }

    if (datasetDeBiasEntity == null) {
      return new DeBiasStatusDTO(Integer.valueOf(datasetId), debiasState, ZonedDateTime.now(), totalRecordsToDebias,
          totalRecordsDebiased);
    } else {
      return new DeBiasStatusDTO(Integer.valueOf(datasetId), debiasState,
          datasetDeBiasEntity.getCreatedDate(), totalRecordsToDebias, totalRecordsDebiased);
    }
  }

  /**
   * Creates or retrieves a {@link DatasetDeBiasEntity} for the specified dataset ID.
   *
   * <p>If an existing {@link DatasetDeBiasEntity} for the given dataset ID is found, it is returned.
   * Otherwise, a new entity is created with an initial state of {@code DebiasState.READY} and saved.
   *
   * @param datasetId the ID of the dataset for which the debias entity is to be created or retrieved
   * @return the created or retrieved {@link DatasetDeBiasEntity} for the specified dataset
   */
  public @NotNull DatasetDeBiasEntity createDatasetDeBiasEntity(String datasetId) {
    DatasetEntity dataset = datasetRepository.findById(Integer.valueOf(datasetId)).orElseThrow();
    DatasetDeBiasEntity datasetDeBiasEntity = datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(
        Integer.valueOf(datasetId));
    if (datasetDeBiasEntity == null) {
      datasetDeBiasEntity = new DatasetDeBiasEntity(dataset, DebiasState.READY, ZonedDateTime.now());
      datasetDeBiasEntity = datasetDeBiasRepository.save(datasetDeBiasEntity);
    }
    return datasetDeBiasEntity;
  }

  /**
   * Retrieves a list of debias report rows based on dataset ID by querying and processing data from database entities.
   *
   * <p>Fetches main and detail records from the database using the dataset ID, processes the retrieved data into
   * structured debias report rows, and returns the list.
   *
   * @param datasetId the ID of the dataset for which the debias report rows are generated
   * @return a list of {@link DeBiasReportRow} containing the processed debias report data
   */
  private List<DeBiasReportRow> generateDebiasReportRows(String datasetId) {
    List<DeBiasReportRow> reportRows = new ArrayList<>();
    List<RecordDeBiasMainEntity> recordDeBiasMainEntities = this.recordDeBiasMainRepository.findByDatasetId_DatasetId(
        Integer.valueOf(datasetId));

    recordDeBiasMainEntities.forEach(recordDeBiasMainEntity -> {
      List<RecordDeBiasDetailEntity> detailEntities = this.recordDeBiasDetailRepository.findByDebiasIdId(
          recordDeBiasMainEntity.getId());
      ValueDetection valueDetection = new ValueDetection();
      List<Tag> tags = new ArrayList<>();
      detailEntities.forEach(recordDeBiasDetailEntity -> {
        Tag tag = new Tag();
        tag.setStart(recordDeBiasDetailEntity.getTagStart());
        tag.setEnd(recordDeBiasDetailEntity.getTagEnd());
        tag.setLength(recordDeBiasDetailEntity.getTagLength());
        tag.setUri(recordDeBiasDetailEntity.getTagUri());
        tags.add(tag);
      });
      valueDetection.setLiteral(recordDeBiasMainEntity.getLiteral());
      valueDetection.setLanguage(recordDeBiasMainEntity.getLanguage().name().toLowerCase(Locale.US));
      valueDetection.setTags(tags);
      reportRows.add(
          new DeBiasReportRow(recordDeBiasMainEntity.getRecordId(), valueDetection, recordDeBiasMainEntity.getSourceField()));
    });

    return reportRows;
  }

  /**
   * Clean DeBias report.
   *
   * @param datasetId the dataset id
   */
  @Transactional
  public void remove(String datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");
    if (isBlank(datasetId)) {
      throw new IllegalArgumentException("Dataset id must not be empty");
    }
    this.recordDeBiasDetailRepository.deleteByDatasetId(datasetId);
    this.recordDeBiasMainRepository.deleteByDatasetId(datasetId);
    this.datasetDeBiasRepository.deleteByDatasetId(datasetId);
  }
}
