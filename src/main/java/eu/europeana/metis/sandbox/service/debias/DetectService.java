package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.dto.debias.DetectionInfoDto;

public interface DetectService {

  void fail(String datasetId);

  void success(String datasetId);

  boolean process(String datasetId);

  Stateful getState();

  void setState(Stateful state);

  Stateful getReady();

  Stateful getProcessing();

  Stateful getCompleted();

  Stateful getError();

  DetectionInfoDto getDetectionInfo(String datasetId);
}
