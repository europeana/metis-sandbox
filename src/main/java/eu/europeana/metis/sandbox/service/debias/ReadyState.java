package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Record.RecordBuilder;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.debias.DetectionEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;
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
  private final RecordLogRepository recordLogRepository;
  private final RecordPublishable recordPublishable;

  /**
   * Instantiates a new Ready state.
   *
   * @param debiasMachine the debias machine
   * @param detectRepository the detect repository
   */
  public ReadyState(DetectService debiasMachine,
      DetectRepository detectRepository,
      DatasetRepository datasetRepository,
      RecordLogRepository recordLogRepository,
      RecordPublishable recordPublishable) {
    this.stateMachine = debiasMachine;
    this.name = STATE_NAME;
    this.detectRepository = detectRepository;
    this.datasetRepository = datasetRepository;
    this.recordLogRepository = recordLogRepository;
    this.recordPublishable = recordPublishable;
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
      DatasetEntity dataset = getDatasetAndProcessDetectionEntity(datasetId);

      processDatasetAndPublishToDeBiasReadyQueue(dataset);

      success(datasetId);

      LOGGER.info("success {} {}", STATE_NAME, datasetId);
    } catch (RuntimeException e) {
      fail(datasetId);
      LOGGER.warn("fail {} {}", STATE_NAME, datasetId, e);
      return false;
    }
    return this.stateMachine.process(datasetId);
  }

  private @NotNull DatasetEntity getDatasetAndProcessDetectionEntity(Integer datasetId) {
    DatasetEntity dataset = datasetRepository.findById(datasetId).orElseThrow();
    DetectionEntity detectionEntity = detectRepository.findDetectionEntityByDatasetId_DatasetId(datasetId);
    if (detectionEntity == null) {
      detectionEntity = new DetectionEntity(dataset, STATE_NAME);
      detectRepository.save(detectionEntity);
    } else {
      detectRepository.updateState(datasetId, STATE_NAME);
    }
    return dataset;
  }

  private void processDatasetAndPublishToDeBiasReadyQueue(DatasetEntity dataset) {
    this.recordLogRepository.findRecordLogByDatasetIdAndStep(dataset.getDatasetId().toString(), Step.VALIDATE_INTERNAL)
                            .parallelStream()
                            .map(r -> {
                              LOGGER.info("Records in: {} :: {}", STATE_NAME, r.getRecordId());
                                return new RecordInfo(new RecordBuilder()
                                    .recordId(r.getRecordId().getId())
                                    .providerId(r.getRecordId().getProviderId())
                                    .europeanaId(r.getRecordId().getEuropeanaId())
                                    .datasetId(r.getRecordId().getDatasetId())
                                    .datasetName(dataset.getDatasetName())
                                    .country(dataset.getCountry())
                                    .language(dataset.getLanguage())
                                    .content(r.getContent().getBytes(StandardCharsets.UTF_8))
                                    .build(), new ArrayList<>());}
                            )
                            .forEach(recordPublishable::publishToDeBiasQueue);
  }

}
