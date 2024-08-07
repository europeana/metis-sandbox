package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.dto.debias.DetectionInfoDto;
import eu.europeana.metis.sandbox.entity.debias.DetectionEntity;
import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import java.time.ZonedDateTime;

/**
 * The type DeBias detect service.
 */
public class DebiasDetectService implements DetectService {

  private static final String INITIAL_STATE = "READY";
  private final Stateful ready;
  private final Stateful processing;
  private final Stateful completed;
  private final Stateful error;
  private final DetectRepository detectRepository;
  private Stateful state;

  /**
   * Instantiates a new DeBias detect service.
   *
   * @param detectRepository the detect repository
   */
  public DebiasDetectService(DetectRepository detectRepository) {
    this.ready = new ReadyState(this, detectRepository);
    this.processing = new ProcessingState(this, detectRepository);
    this.completed = new CompletedState(this, detectRepository);
    this.error = new ErrorState(this, detectRepository);
    this.state = this.ready;
    this.detectRepository = detectRepository;
  }

  @Override
  public void fail(Integer datasetId) {
    state.fail(datasetId);
  }

  @Override
  public void success(Integer datasetId) {
    state.success(datasetId);
  }

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

  /**
   * Gets DeBias detection info.
   *
   * @param datasetId the dataset id
   * @return the DeBias detection info
   */
  @Override
  public DetectionInfoDto getDetectionInfo(Integer datasetId) {
    DetectionEntity detectionEntity = detectRepository.findDetectionEntityByDatasetId_DatasetId(datasetId);
    if (detectionEntity == null) {
      return new DetectionInfoDto(datasetId, INITIAL_STATE, ZonedDateTime.now());
    } else {
      return new DetectionInfoDto(datasetId, detectionEntity.getState(), detectionEntity.getCreatedDate());
    }
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
}
