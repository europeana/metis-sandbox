package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.entity.debias.DetectionEntity;
import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import org.springframework.transaction.annotation.Transactional;

public class CompletedState extends State implements Stateful {

  private static final String STATE_NAME = "COMPLETED";

  public CompletedState(DetectService debiasMachine, DetectRepository detectRepository) {
    this.stateMachine = debiasMachine;
    this.name = STATE_NAME;
    this.detectRepository = detectRepository;
    this.terminalState = true;
  }

  @Override
  public void fail(String datasetId) {
    this.stateMachine.setState(this.stateMachine.getError());
  }

  @Override
  public void success(String datasetId) {
    // do nothing, processing completed.
  }

  @Transactional
  @Override
  public boolean process(String datasetId) {
    LOGGER.info("{} {}", STATE_NAME, datasetId);
    try {
      DetectionEntity detectionEntity = detectRepository.findByDatasetId(datasetId);
      if (detectionEntity == null) {
        fail(datasetId);
        LOGGER.warn("invalid state {} {}", STATE_NAME, datasetId);
        return this.stateMachine.process(datasetId);
      } else {
        detectRepository.updateState(datasetId, STATE_NAME);
        success(datasetId);
        LOGGER.info("success {} {}", STATE_NAME, datasetId);
      }

    } catch (RuntimeException e) {
      fail(datasetId);
      LOGGER.warn("fail {} {}", STATE_NAME, datasetId, e);
      return this.stateMachine.process(datasetId);
    }
    return terminalState;
  }

  public String getName() {
    return STATE_NAME;
  }
}
