package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.dto.debias.DetectionInfoDto;
import eu.europeana.metis.sandbox.entity.debias.DetectionEntity;
import eu.europeana.metis.sandbox.repository.debias.DetectRepository;

public class DebiasMachineService implements DetectService {

  private final Stateful ready;
  private final Stateful processing;
  private final Stateful completed;
  private final Stateful error;
  private final DetectRepository detectRepository;
  private Stateful state;

  public DebiasMachineService(DetectRepository detectRepository) {
    this.ready = new ReadyState(this, detectRepository);
    this.processing = new ProcessingState(this, detectRepository);
    this.completed = new CompletedState(this, detectRepository);
    this.error = new ErrorState(this, detectRepository);
    this.state = this.ready;
    this.detectRepository = detectRepository;
  }

  @Override
  public void fail(Long datasetId) {
    state.fail(datasetId);
  }

  @Override
  public void success(Long datasetId) {
    state.success(datasetId);
  }

  @Override
  public boolean process(Long datasetId) {
    return state.process(datasetId);
  }

  @Override
  public Stateful getState() {
    return state;
  }

  /**
   * Gets debias detection info.
   *
   * @param datasetId the dataset id
   * @return the debias detection info
   */
  @Override
  public DetectionInfoDto getDetectionInfo(Long datasetId) {
    DetectionEntity detectionEntity = detectRepository.findDetectionEntityByDatasetId_DatasetId(datasetId);
    return new DetectionInfoDto(datasetId, detectionEntity.getState(), detectionEntity.getCreatedDate());
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
}
