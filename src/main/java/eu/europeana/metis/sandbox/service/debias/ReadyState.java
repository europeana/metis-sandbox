package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.debias.DetectionEntity;
import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Ready state.
 */
public class ReadyState extends State implements Stateful {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReadyState.class);
  private static final String STATE_NAME = "READY";

  /**
   * Instantiates a new Ready state.
   *
   * @param debiasMachine the debias machine
   * @param detectRepository the detect repository
   */
  public ReadyState(DetectService debiasMachine,
      DetectRepository detectRepository) {
    this.stateMachine = debiasMachine;
    this.name = STATE_NAME;
    this.detectRepository = detectRepository;
    this.terminalState = false;
  }

  @Override
  public void fail(Integer datasetId) {
    this.stateMachine.setState(this.stateMachine.getReady());
  }

  @Override
  public void success(Integer datasetId) {
    this.stateMachine.setState(this.stateMachine.getProcessing());
  }

  @Transactional
  @Override
  public boolean process(Integer datasetId) {
    LOGGER.info("{} {}", STATE_NAME, datasetId);
    try {
      DetectionEntity detectionEntity = detectRepository.findDetectionEntityByDatasetId_DatasetId(datasetId);
      if (detectionEntity == null) {
        detectionEntity = new DetectionEntity();
        detectionEntity.setState(STATE_NAME);
        DatasetEntity dataset = new DatasetEntity();
        dataset.setDatasetId(datasetId);
        detectionEntity.setDatasetId(dataset);
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
