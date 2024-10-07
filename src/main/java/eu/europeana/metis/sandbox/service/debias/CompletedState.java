package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import eu.europeana.metis.sandbox.repository.debias.DatasetDeBiasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Completed state.
 */
public class CompletedState extends State implements Stateful {

  private static final Logger LOGGER = LoggerFactory.getLogger(CompletedState.class);
  private static final String STATE_NAME = "COMPLETED";

  /**
   * Instantiates a new Completed state.
   *
   * @param debiasMachine the debias machine
   * @param datasetDeBiasRepository the detect repository
   */
  public CompletedState(DeBiasStateful debiasMachine, DatasetDeBiasRepository datasetDeBiasRepository) {
    this.stateMachine = debiasMachine;
    this.name = STATE_NAME;
    this.datasetDeBiasRepository = datasetDeBiasRepository;
    this.terminalState = true;
  }

  @Override
  public void fail(Integer datasetId) {
    this.stateMachine.setState(this.stateMachine.getError());
  }

  @Override
  public void success(Integer datasetId) {
    // do nothing, processing completed.
  }

  @Transactional
  @Override
  public boolean process(Integer datasetId) {
    LOGGER.info("{} {}", STATE_NAME, datasetId);
    try {
      DatasetDeBiasEntity datasetDeBiasEntity = datasetDeBiasRepository.findDetectionEntityByDatasetId_DatasetId(datasetId);
      if (datasetDeBiasEntity == null) {
        fail(datasetId);
        LOGGER.warn("invalid state {} {}", STATE_NAME, datasetId);
        return this.stateMachine.process(datasetId);
      } else {
        datasetDeBiasRepository.updateState(datasetId, STATE_NAME);
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

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return STATE_NAME;
  }
}
