package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import org.springframework.transaction.annotation.Transactional;

public class ProcessingState extends State implements Stateful {

  private static final String STATE_NAME = "PROCESSING";

  public ProcessingState(DetectService debiasMachine, DetectRepository detectRepository) {
    this.stateMachine = debiasMachine;
    this.name = STATE_NAME;
    this.detectRepository = detectRepository;
    this.terminalState = false;
  }

  @Override
  public void fail(String datasetId) {
    this.stateMachine.setState(this.stateMachine.getError());
  }

  @Override
  public void success(String datasetId) {
    this.stateMachine.setState(this.stateMachine.getCompleted());
  }

  @Transactional
  @Override
  public boolean process(String datasetId) {
    LOGGER.info("{} {}", STATE_NAME, datasetId);
    try {
      detectRepository.updateState(datasetId, STATE_NAME);
      // TODO: add logic

      success(datasetId);
      LOGGER.info("success {} {}", STATE_NAME, datasetId);
    } catch (RuntimeException e) {
      fail(datasetId);
      LOGGER.warn("fail {} {}", STATE_NAME, datasetId, e);
      // TODO: add retryable logic future.
    }
    return this.stateMachine.process(datasetId);
  }

  public String getName() {
    return STATE_NAME;
  }
}
