package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.debias.DetectionEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
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
  private final DatasetRepository datasetRepository;

  /**
   * Instantiates a new Ready state.
   *
   * @param debiasMachine the debias machine
   * @param detectRepository the detect repository
   */
  public ReadyState(DetectService debiasMachine,
      DetectRepository detectRepository, DatasetRepository datasetRepository) {
    this.stateMachine = debiasMachine;
    this.name = STATE_NAME;
    this.detectRepository = detectRepository;
    this.datasetRepository = datasetRepository;
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
      DatasetEntity dataset = datasetRepository.findById(datasetId).orElseThrow();
      DetectionEntity detectionEntity = detectRepository.findDetectionEntityByDatasetId_DatasetId(datasetId);
      if (detectionEntity == null) {
        detectionEntity = new DetectionEntity(dataset, STATE_NAME);
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
