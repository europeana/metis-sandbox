package eu.europeana.metis.sandbox.service.debias;

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
    // do nothing
  }

  @Override
  public void success(String datasetId) {
    // do nothing
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
      return false;
    }
    return true;
  }

  public String getName() {
    return STATE_NAME;
  }
}
