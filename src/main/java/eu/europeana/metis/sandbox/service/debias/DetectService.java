package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.dto.debias.DetectionInfoDto;

public interface DetectService {

  void fail(Long datasetId);

  void success(Long datasetId);

  boolean process(Long datasetId);

  Stateful getState();

  void setState(Stateful state);

  Stateful getReady();

  Stateful getProcessing();

  Stateful getCompleted();

  Stateful getError();

  DetectionInfoDto getDetectionInfo(Long datasetId);
}
