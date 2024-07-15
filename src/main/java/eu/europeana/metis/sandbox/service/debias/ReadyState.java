package eu.europeana.metis.sandbox.service.debias;

import static java.lang.String.format;

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
  public void fail(String datasetId) {
    this.stateMachine.setState(this.stateMachine.getReady());
  }

  @Override
  public void success(String datasetId) {
    this.stateMachine.setState(this.stateMachine.getProcessing());
  }

  @Transactional
  @Override
  public boolean process(String datasetId) {
    LOGGER.info("{} {}", STATE_NAME, datasetId);
    try {
      detectRepository.findByDatasetId(datasetId);
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
}
