package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import eu.europeana.metis.sandbox.repository.debias.DatasetDeBiasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Processing state.
 */
public class ProcessingState extends State implements Stateful {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingState.class);
  private static final String STATE_NAME = "PROCESSING";

  /**
   * Instantiates a new Processing state.
   *
   * @param debiasMachine the debias machine
   * @param datasetDeBiasRepository the detect repository
   */
  public ProcessingState(DetectService debiasMachine, DatasetDeBiasRepository datasetDeBiasRepository) {
    this.stateMachine = debiasMachine;
    this.name = STATE_NAME;
    this.datasetDeBiasRepository = datasetDeBiasRepository;
    this.terminalState = false;
  }

  @Override
  public void fail(Integer datasetId) {
    this.stateMachine.setState(this.stateMachine.getError());
  }

  @Override
  public void success(Integer datasetId) {
    this.stateMachine.setState(this.stateMachine.getCompleted());
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
      } else {
        datasetDeBiasRepository.updateState(datasetId, STATE_NAME);
        success(datasetId);
        LOGGER.info("success {} {}", STATE_NAME, datasetId);
        // TODO: processing logic here
      }
    } catch (RuntimeException e) {
      fail(datasetId);
      LOGGER.warn("fail {} {}", STATE_NAME, datasetId, e);
    }
    return this.stateMachine.process(datasetId);
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
