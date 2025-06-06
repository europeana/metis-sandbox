package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.debias.detect.model.response.Tag;
import eu.europeana.metis.debias.detect.model.response.ValueDetection;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.config.batch.DebiasJobConfig;
import eu.europeana.metis.sandbox.dto.debias.DeBiasReportDTO;
import eu.europeana.metis.sandbox.dto.debias.DeBiasStatusDTO;
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
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type DeBias detect service.
 */
public class DeBiasStateService {

  private static final String READY_STATE = "READY";
  private final DatasetDeBiasRepository datasetDeBiasRepository;
  private final RecordDeBiasMainRepository recordDeBiasMainRepository;
  private final RecordDeBiasDetailRepository recordDeBiasDetailRepository;
  private final DatasetRepository datasetRepository;
  private final ExecutionRecordRepository executionRecordRepository;

  /**
   * Instantiates a new DeBias detect service.
   *
   * @param datasetDeBiasRepository the detect repository
   * @param datasetRepository the dataset repository
   * @param recordDeBiasMainRepository the record de bias main repository
   * @param recordDeBiasDetailRepository the record de bias detail repository
   */
  public DeBiasStateService(DatasetDeBiasRepository datasetDeBiasRepository,
      DatasetRepository datasetRepository,
      RecordDeBiasMainRepository recordDeBiasMainRepository,
      RecordDeBiasDetailRepository recordDeBiasDetailRepository, ExecutionRecordRepository executionRecordRepository) {
    this.datasetDeBiasRepository = datasetDeBiasRepository;
    this.recordDeBiasMainRepository = recordDeBiasMainRepository;
    this.recordDeBiasDetailRepository = recordDeBiasDetailRepository;
    this.datasetRepository = datasetRepository;
    this.executionRecordRepository = executionRecordRepository;
  }

  public DeBiasReportDTO getDeBiasReport(String datasetId) {
    DeBiasStatusDTO deBiasStatusDto = getDeBiasStatus(datasetId);
    return new DeBiasReportDTO(Integer.valueOf(datasetId), deBiasStatusDto.getState(),
        deBiasStatusDto.getCreationDate(), deBiasStatusDto.getTotal(),
        deBiasStatusDto.getProcessed(), getReportFromDbEntities(datasetId));

  }

  /**
   * Clean DeBias report.
   *
   * @param datasetId the dataset id
   */
  @Transactional
  public void remove(String datasetId) {
    Objects.requireNonNull(datasetId, "Dataset id must not be null");
    this.recordDeBiasDetailRepository.deleteByDatasetId(datasetId);
    this.recordDeBiasMainRepository.deleteByDatasetId(datasetId);
    this.datasetDeBiasRepository.deleteByDatasetId(datasetId);
  }

  public DeBiasStatusDTO getDeBiasStatus(String datasetId) {
    DatasetDeBiasEntity datasetDeBiasEntity = datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(
        Integer.valueOf(datasetId));
    long totalToDebias = executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        FullBatchJobType.VALIDATE_INTERNAL.name());
    long progressDebias = executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        DebiasJobConfig.BATCH_JOB.name());

    final String state;
    if (totalToDebias > 0 && (totalToDebias == progressDebias)) {
      state = "COMPLETED";
    } else if (totalToDebias >= 0 && progressDebias == 0) {
      state = READY_STATE;
    } else if (totalToDebias > 0 && progressDebias > 0) {
      state = "PROCESSING";
    } else {
      state = "INVALID";
    }

    if (datasetDeBiasEntity == null) {
      return new DeBiasStatusDTO(Integer.valueOf(datasetId), state, ZonedDateTime.now(), totalToDebias, progressDebias);
    } else {
      return new DeBiasStatusDTO(Integer.valueOf(datasetId), state,
          datasetDeBiasEntity.getCreatedDate(), totalToDebias, progressDebias);
    }
  }

  public @NotNull DatasetDeBiasEntity createDatasetDeBiasEntity(String datasetId) {
    DatasetEntity dataset = datasetRepository.findById(Integer.valueOf(datasetId)).orElseThrow();
    DatasetDeBiasEntity datasetDeBiasEntity = datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(Integer.valueOf(datasetId));
    if (datasetDeBiasEntity == null) {
      datasetDeBiasEntity = new DatasetDeBiasEntity(dataset, READY_STATE, ZonedDateTime.now());
      datasetDeBiasEntity = datasetDeBiasRepository.save(datasetDeBiasEntity);
    }
    return datasetDeBiasEntity;
  }

  private List<DeBiasReportRow> getReportFromDbEntities(String datasetId) {
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
}
