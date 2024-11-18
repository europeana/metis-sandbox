package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.debias.detect.model.response.Tag;
import eu.europeana.metis.debias.detect.model.response.ValueDetection;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Record.RecordBuilder;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.dto.debias.DeBiasReportDto;
import eu.europeana.metis.sandbox.dto.debias.DeBiasStatusDto;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasDetailEntity;
import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasMainEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.debias.DatasetDeBiasRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasDetailRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasMainRepository;
import eu.europeana.metis.sandbox.service.workflow.DeBiasProcessServiceImpl.DeBiasReportRow;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type DeBias detect service.
 */
public class DeBiasStateServiceImpl implements DeBiasStateService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeBiasStateServiceImpl.class);
  private static final String READY_STATE = "READY";
  private final DatasetDeBiasRepository datasetDeBiasRepository;
  private final RecordDeBiasMainRepository recordDeBiasMainRepository;
  private final RecordDeBiasDetailRepository recordDeBiasDetailRepository;
  private final DatasetRepository datasetRepository;
  private final RecordLogRepository recordLogRepository;
  private final RecordDeBiasPublishable recordDeBiasPublishable;
  private final Map<Integer, Lock> datasetIdLocksMap = new ConcurrentHashMap<>();
  private final LockRegistry lockRegistry;

  /**
   * Instantiates a new DeBias detect service.
   *
   * @param datasetDeBiasRepository the detect repository
   * @param datasetRepository the dataset repository
   * @param recordLogRepository the record log repository
   * @param recordDeBiasPublishable the record publishable
   * @param recordDeBiasMainRepository the record de bias main repository
   * @param recordDeBiasDetailRepository the record de bias detail repository
   * @param lockRegistry the lock registry
   */
  public DeBiasStateServiceImpl(DatasetDeBiasRepository datasetDeBiasRepository,
      DatasetRepository datasetRepository,
      RecordLogRepository recordLogRepository,
      RecordDeBiasPublishable recordDeBiasPublishable,
      RecordDeBiasMainRepository recordDeBiasMainRepository,
      RecordDeBiasDetailRepository recordDeBiasDetailRepository,
      LockRegistry lockRegistry) {
    this.datasetDeBiasRepository = datasetDeBiasRepository;
    this.recordDeBiasMainRepository = recordDeBiasMainRepository;
    this.recordDeBiasDetailRepository = recordDeBiasDetailRepository;
    this.datasetRepository = datasetRepository;
    this.recordLogRepository = recordLogRepository;
    this.recordDeBiasPublishable = recordDeBiasPublishable;
    this.lockRegistry = lockRegistry;
  }

  @Transactional
  @Override
  public boolean process(Integer datasetId) {
    final Lock lock = datasetIdLocksMap.computeIfAbsent(datasetId, s -> lockRegistry.obtain("debiasProcess_" + datasetId));
    LOGGER.info("{} {}", READY_STATE, datasetId);

    try {
      lock.lock();
      LOGGER.info("DeBias processing: {} lock, Locked", datasetId);
      DatasetDeBiasEntity dataset = getDatasetDeBiasEntity(datasetId);

      processDatasetAndPublishToDeBiasReadyQueue(dataset);

      LOGGER.info("success {} {}", READY_STATE, datasetId);
    } catch (RuntimeException e) {
      LOGGER.warn("fail {} {}", READY_STATE, datasetId, e);
      return false;
    } finally {
      lock.unlock();
      LOGGER.info("DeBias processing: {} lock, Unlocked", datasetId);
    }
    return true;
  }

  /**
   * Gets DeBias report.
   *
   * @param datasetId the dataset id
   * @return the de bias report
   */
  @Override
  public DeBiasReportDto getDeBiasReport(Integer datasetId) {
    DeBiasStatusDto deBiasStatusDto = getDeBiasStatus(datasetId);
    if (READY_STATE.equals(deBiasStatusDto.getState())) {
      return new DeBiasReportDto(datasetId, deBiasStatusDto.getState(),
          deBiasStatusDto.getCreationDate(), deBiasStatusDto.getTotal(),
          deBiasStatusDto.getProcessed(), List.of());
    } else {
      return new DeBiasReportDto(datasetId, deBiasStatusDto.getState(),
          deBiasStatusDto.getCreationDate(), deBiasStatusDto.getTotal(),
          deBiasStatusDto.getProcessed(), getReportFromDbEntities(datasetId));
    }
  }

  /**
   * Clean DeBias report.
   *
   * @param datasetId the dataset id
   */
  @Transactional
  @Override
  public void cleanDeBiasReport(Integer datasetId) {
    Objects.requireNonNull(datasetId, "Dataset id must not be null");
    this.recordDeBiasDetailRepository.deleteByDebiasIdRecordIdDatasetId(datasetId.toString());
    this.recordDeBiasMainRepository.deleteByRecordIdDatasetId(datasetId.toString());
    this.datasetDeBiasRepository.deleteByDatasetId(datasetId.toString());
  }

  /**
   * Get DeBias status.
   *
   * @param datasetId the dataset id
   */
  @Override
  public DeBiasStatusDto getDeBiasStatus(Integer datasetId) {
    DatasetDeBiasEntity datasetDeBiasEntity = datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(datasetId);
    final int progressDeBias = recordLogRepository.getProgressDeBiasCounterByDatasetId(datasetId.toString());
    final int totalDeBias = recordLogRepository.getTotalDeBiasCounterByDatasetId(datasetId.toString());
    if (datasetDeBiasEntity == null) {
      return new DeBiasStatusDto(datasetId, READY_STATE, ZonedDateTime.now(), totalDeBias, progressDeBias);
    } else {
      return new DeBiasStatusDto(datasetId, datasetDeBiasEntity.getState(),
          datasetDeBiasEntity.getCreatedDate(), totalDeBias, progressDeBias);
    }
  }

  private @NotNull DatasetDeBiasEntity getDatasetDeBiasEntity(Integer datasetId) {
    DatasetEntity dataset = datasetRepository.findById(datasetId).orElseThrow();
    DatasetDeBiasEntity datasetDeBiasEntity = datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(datasetId);
    if (datasetDeBiasEntity == null) {
      datasetDeBiasEntity = new DatasetDeBiasEntity(dataset, READY_STATE);
      datasetDeBiasEntity = datasetDeBiasRepository.save(datasetDeBiasEntity);
    } else {
      datasetDeBiasEntity.setState(READY_STATE);
      datasetDeBiasRepository.updateState(datasetId, READY_STATE);
    }
    return datasetDeBiasEntity;
  }

  private void processDatasetAndPublishToDeBiasReadyQueue(DatasetDeBiasEntity dataset) {
    // clean up any previous processing.
    this.recordLogRepository.deleteByRecordIdDatasetIdAndStep(dataset.getDatasetId().getDatasetId().toString(), Step.DEBIAS);
    // start a new processing from validated records.
    this.recordLogRepository.findRecordLogByDatasetIdAndStep(dataset.getDatasetId().getDatasetId().toString(), Step.NORMALIZE)
                            .parallelStream()
                            .map(r -> {
                                  LOGGER.debug("DeBias records in: {} :: {}", READY_STATE, r.getRecordId());
                                  return new RecordInfo(new RecordBuilder()
                                      .recordId(r.getRecordId().getId())
                                      .providerId(r.getRecordId().getProviderId())
                                      .europeanaId(r.getRecordId().getEuropeanaId())
                                      .datasetId(r.getRecordId().getDatasetId())
                                      .datasetName(dataset.getDatasetId().getDatasetName())
                                      .country(dataset.getDatasetId().getCountry())
                                      .language(dataset.getDatasetId().getLanguage())
                                      .content(r.getContent().getBytes(StandardCharsets.UTF_8))
                                      .build(), new ArrayList<>());
                                }
                            )
                            .forEach(recordDeBiasPublishable::publishToDeBiasQueue);
  }

  private List<DeBiasReportRow> getReportFromDbEntities(Integer datasetId) {
    List<DeBiasReportRow> reportRows = new ArrayList<>();
    List<RecordDeBiasMainEntity> recordDeBiasMainEntities = this.recordDeBiasMainRepository.findByRecordIdDatasetId(
        datasetId.toString());

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
      reportRows.add(new DeBiasReportRow(recordDeBiasMainEntity.getRecordId().getId(),
          recordDeBiasMainEntity.getRecordId().getEuropeanaId(),
          valueDetection,
          recordDeBiasMainEntity.getSourceField()));
    });

    return reportRows;
  }
}
