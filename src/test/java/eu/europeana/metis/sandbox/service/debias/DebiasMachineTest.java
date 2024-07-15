package eu.europeana.metis.sandbox.service.debias;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.debias.DetectionEntity;
import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DebiasMachineTest {

  @Mock
  DetectRepository detectRepository;
  @InjectMocks
  DebiasMachine debiasMachine;

  @Test
  void fail() {

  }

  @Test
  void success() {
  }

  @Test
  void processWhenNewHappyPath() {
    String datasetId = "datasetId";
    when(detectRepository.findByDatasetId(anyString())).thenReturn(null);

    debiasMachine.process(datasetId);

    verify(detectRepository, times(3)).updateState(anyString(), anyString());
  }

  @Test
  void processWhenExists() {
    String datasetId = "1";
    String state = "READY";
    DatasetEntity dataset = new DatasetEntity();
    dataset.setDatasetId(1);
    DetectionEntity detectionEntity = new DetectionEntity(dataset, state);
    when(detectRepository.findByDatasetId(anyString())).thenReturn(detectionEntity);

    debiasMachine.process(datasetId);

    verify(detectRepository, times(3)).updateState(anyString(), anyString());
  }

  @Test
  void getState() {
  }

  @Test
  void setState() {
  }

  @Test
  void getReady() {
  }

  @Test
  void getProcessing() {
  }

  @Test
  void getCompleted() {
  }

  @Test
  void getError() {
  }
}
