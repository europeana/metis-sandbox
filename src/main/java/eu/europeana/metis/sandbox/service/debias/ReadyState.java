package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.entity.debias.DetectionEntity;
import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import org.springframework.transaction.annotation.Transactional;

public class ReadyState extends State implements Stateful {

  private static final String STATE_NAME = "READY";

  public ReadyState(DetectService debiasMachine,
      DetectRepository detectRepository) {
    this.stateMachine = debiasMachine;
    this.name = STATE_NAME;
    this.detectRepository = detectRepository;
    this.terminalState = false;
  }

  @Override
  public void fail(Long datasetId) {
    this.stateMachine.setState(this.stateMachine.getReady());
  }

  @Override
  public void success(Long datasetId) {
    this.stateMachine.setState(this.stateMachine.getProcessing());
  }

  @Transactional
  @Override
  public boolean process(Long datasetId) {
    LOGGER.info("{} {}", STATE_NAME, datasetId);
    try {
      DetectionEntity detectionEntity = detectRepository.findDetectionEntityByDatasetId_DatasetId(datasetId);
      if (detectionEntity == null) {
        detectionEntity = new DetectionEntity();
        detectionEntity.setState(STATE_NAME);
        detectionEntity.setId(datasetId);
        detectRepository.save(detectionEntity);
      } else {
        detectRepository.updateState(datasetId, STATE_NAME);
      }
      success(datasetId);
      LOGGER.info("success {} {}", STATE_NAME, datasetId);
    } catch (RuntimeException e) {
      fail(datasetId);
      LOGGER.warn("fail {} {}", STATE_NAME, datasetId, e);
      return false;
    }
    return this.stateMachine.process(datasetId);
  }
}
