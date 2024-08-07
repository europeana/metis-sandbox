package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.entity.debias.DetectionEntity;
import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Error state.
 */
public class ErrorState extends State implements Stateful {

  private static final Logger LOGGER = LoggerFactory.getLogger(ErrorState.class);
  private static final String STATE_NAME = "ERROR";

  /**
   * Instantiates a new Error state.
   *
   * @param debiasMachine the debias machine
   * @param detectRepository the detect repository
   */
  public ErrorState(DetectService debiasMachine, DetectRepository detectRepository) {
    this.stateMachine = debiasMachine;
    this.name = STATE_NAME;
    this.detectRepository = detectRepository;
    this.terminalState = false;
  }

  @Override
  public void fail(Integer datasetId) {
    this.stateMachine.setState(this.stateMachine.getError());
  }

  @Override
  public void success(Integer datasetId) {
    this.stateMachine.setState(this.stateMachine.getReady());
  }

  @Transactional
  @Override
  public boolean process(Integer datasetId) {
    LOGGER.info("{} {}", STATE_NAME, datasetId);
    try {
      DetectionEntity detectionEntity = detectRepository.findDetectionEntityByDatasetId_DatasetId(datasetId);
      if (detectionEntity == null) {
        fail(datasetId);
        LOGGER.warn("invalid state {} {}", STATE_NAME, datasetId);
        return false;
      } else {
        detectRepository.updateState(datasetId, STATE_NAME);
        success(datasetId);
        LOGGER.info("success {} {}", STATE_NAME, datasetId);
      }
    } catch (RuntimeException e) {
      fail(datasetId);
      LOGGER.warn("fail {} {}", STATE_NAME, datasetId, e);
      return false;
    }
    return this.stateMachine.process(datasetId);
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return this.name;
  }
}
