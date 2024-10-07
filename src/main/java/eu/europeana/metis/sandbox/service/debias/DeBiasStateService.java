package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.dto.debias.DetectionInfoDto;
import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.debias.DatasetDeBiasRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasDetailRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasMainRepository;
import java.time.ZonedDateTime;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type DeBias detect service.
 */
public class DeBiasStateService implements DetectService {

  private static final String INITIAL_STATE = "READY";
  private final Stateful ready;
  private final Stateful processing;
  private final Stateful completed;
  private final Stateful error;
  private final DatasetDeBiasRepository datasetDeBiasRepository;
  private final RecordDeBiasMainRepository recordDeBiasMainRepository;
  private final RecordDeBiasDetailRepository recordDeBiasDetailRepository;
  private Stateful state;

  /**
   * Instantiates a new DeBias detect service.
   *
   * @param datasetDeBiasRepository the detect repository
   * @param datasetRepository the dataset repository
   * @param recordLogRepository the record log repository
   * @param recordDeBiasPublishable the record publishable
   */
  public DeBiasStateService(DatasetDeBiasRepository datasetDeBiasRepository,
      DatasetRepository datasetRepository,
      RecordLogRepository recordLogRepository,
      RecordDeBiasPublishable recordDeBiasPublishable,
      RecordDeBiasMainRepository recordDeBiasMainRepository,
      RecordDeBiasDetailRepository recordDeBiasDetailRepository) {
    this.ready = new ReadyState(this, datasetDeBiasRepository, datasetRepository, recordLogRepository, recordDeBiasPublishable);
    this.processing = new ProcessingState(this, datasetDeBiasRepository);
    this.completed = new CompletedState(this, datasetDeBiasRepository);
    this.error = new ErrorState(this, datasetDeBiasRepository);
    this.state = this.ready;
    this.datasetDeBiasRepository = datasetDeBiasRepository;
    this.recordDeBiasMainRepository = recordDeBiasMainRepository;
    this.recordDeBiasDetailRepository = recordDeBiasDetailRepository;
  }

  @Override
  public void fail(Integer datasetId) {
    state.fail(datasetId);
  }

  @Override
  public void success(Integer datasetId) {
    state.success(datasetId);
  }

  @Transactional
  @Override
  public boolean process(Integer datasetId) {
    return state.process(datasetId);
  }

  @Override
  public Stateful getState() {
    return state;
  }

  public void setState(Stateful state) {
    this.state = state;
  }

  public Stateful getReady() {
    return ready;
  }

  public Stateful getProcessing() {
    return processing;
  }

  public Stateful getCompleted() {
    return completed;
  }

  public Stateful getError() {
    return error;
  }

  /**
   * Gets DeBias report.
   *
   * @param datasetId the dataset id
   * @return the de bias report
   */
  @Override
  public DetectionInfoDto getDeBiasReport(Integer datasetId) {
    DatasetDeBiasEntity datasetDeBiasEntity = datasetDeBiasRepository.findDetectionEntityByDatasetId_DatasetId(datasetId);
    if (datasetDeBiasEntity == null) {
      return new DetectionInfoDto(datasetId, INITIAL_STATE, ZonedDateTime.now());
    } else {
      return new DetectionInfoDto(datasetId, datasetDeBiasEntity.getState(), datasetDeBiasEntity.getCreatedDate());
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
  }
}
