package eu.europeana.metis.sandbox.service.debias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.dto.debias.DetectionInfoDto;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.debias.DetectionEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeBiasDetectServiceTest {

  @Mock
  DetectRepository detectRepository;

  @Mock
  DatasetRepository datasetRepository;

  @InjectMocks
  DeBiasDetectService debiasDetectService;

  @Test
  void processWhenDatasetNotExists_expectSuccess() {
    final Integer datasetId = 1;
    final DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState("READY");
    detectionEntity.setCreatedDate(ZonedDateTime.now());

    when(datasetRepository.findById(anyInt())).thenThrow(NoSuchElementException.class);

    boolean result = debiasDetectService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ReadyState.class, debiasDetectService.getState());
    verify(detectRepository, times(0)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(0)).save(any(DetectionEntity.class));
    verify(detectRepository, times(0)).updateState(anyInt(), anyString());
  }


  @Test
  void processWhenNewHappyPath_Ready_Processing_Completed_expectSuccess() {
    final Integer datasetId = 1;
    final DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState("READY");
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId);
    detectionEntity.setDatasetId(datasetEntity);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenReturn(null)
        .thenReturn(detectionEntity)
        .thenReturn(detectionEntity);

    boolean result = debiasDetectService.process(datasetId);

    assertTrue(result);
    assertInstanceOf(CompletedState.class, debiasDetectService.getState());
    verify(detectRepository, times(3)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(1)).save(any(DetectionEntity.class));
    verify(detectRepository, times(2)).updateState(anyInt(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_andError_expectSuccess() {
    final Integer datasetId = 1;
    final DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState("READY");
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId);
    detectionEntity.setDatasetId(datasetEntity);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenThrow(new RuntimeException("Error"));

    boolean result = debiasDetectService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ReadyState.class, debiasDetectService.getState());
    verify(detectRepository, times(1)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(0)).save(any(DetectionEntity.class));
    verify(detectRepository, times(0)).updateState(anyInt(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_Processing_Completed_andError_expectSuccess() {
    final Integer datasetId = 1;
    final DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState("READY");
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId);
    detectionEntity.setDatasetId(datasetEntity);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenReturn(null)
        .thenReturn(detectionEntity)
        .thenReturn(null)
        .thenReturn(null);

    boolean result = debiasDetectService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ErrorState.class, debiasDetectService.getState());
    verify(detectRepository, times(4)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(1)).save(any(DetectionEntity.class));
    verify(detectRepository, times(1)).updateState(anyInt(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_Processing_Completed_andException_expectSuccess() {
    final Integer datasetId = 1;
    final DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState("READY");
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId);
    detectionEntity.setDatasetId(datasetEntity);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenReturn(null)
        .thenReturn(detectionEntity)
        .thenThrow(new RuntimeException("Error"))
        .thenReturn(null);

    boolean result = debiasDetectService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ErrorState.class, debiasDetectService.getState());
    verify(detectRepository, times(4)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(1)).save(any(DetectionEntity.class));
    verify(detectRepository, times(1)).updateState(anyInt(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_Processing_Error_Ready_Processing_Completed_expectSuccess() {
    final Integer datasetId = 1;
    final DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState("READY");
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId);
    detectionEntity.setDatasetId(datasetEntity);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenReturn(null)
        .thenThrow(new RuntimeException("Error"))
        .thenReturn(detectionEntity)
        .thenReturn(detectionEntity)
        .thenReturn(detectionEntity)
        .thenReturn(detectionEntity);

    boolean result = debiasDetectService.process(datasetId);

    assertTrue(result);
    assertInstanceOf(CompletedState.class, debiasDetectService.getState());
    verify(detectRepository, times(6)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(1)).save(any(DetectionEntity.class));
    verify(detectRepository, times(4)).updateState(anyInt(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_Processing_andError_expectSuccess() {
    final Integer datasetId = 1;
    final DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState("READY");
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId);
    detectionEntity.setDatasetId(datasetEntity);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null);

    boolean result = debiasDetectService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ErrorState.class, debiasDetectService.getState());
    verify(detectRepository, times(1)).save(any(DetectionEntity.class));
    verify(detectRepository, times(3)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(0)).updateState(anyInt(), anyString());
  }

  @Test
  void processWhenDatasetAlreadyExists_Ready_Processing_Completed_expectSuccess() {
    final Integer datasetId = 1;
    final String stateName = "READY";
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(1);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    final DetectionEntity detectionEntity = new DetectionEntity(datasetEntity, stateName);
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenReturn(detectionEntity)
        .thenReturn(detectionEntity);

    boolean result = debiasDetectService.process(datasetId);

    assertTrue(result);
    assertInstanceOf(CompletedState.class, debiasDetectService.getState());
    verify(detectRepository, times(3)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(3)).updateState(anyInt(), anyString());
  }

  @Test
  void processWhenDatasetAlreadyExists_Ready_Processing_andError_expectSuccess() {
    final Integer datasetId = 1;
    final String stateName = "READY";
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(1);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    final DetectionEntity detectionEntity = new DetectionEntity(datasetEntity, stateName);
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenReturn(detectionEntity)
        .thenReturn(null)
        .thenReturn(null);
    boolean result = debiasDetectService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ErrorState.class, debiasDetectService.getState());

    verify(detectRepository, times(3)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(1)).updateState(anyInt(), anyString());
  }

  @Test
  void set_and_get_State() {
    debiasDetectService.setState(new ReadyState(debiasDetectService, detectRepository, datasetRepository));
    assertInstanceOf(ReadyState.class, debiasDetectService.getState());

    debiasDetectService.setState(new ProcessingState(debiasDetectService, detectRepository));
    assertInstanceOf(ProcessingState.class, debiasDetectService.getState());

    debiasDetectService.setState(new CompletedState(debiasDetectService, detectRepository));
    assertInstanceOf(CompletedState.class, debiasDetectService.getState());

    debiasDetectService.setState(new ErrorState(debiasDetectService, detectRepository));
    assertInstanceOf(ErrorState.class, debiasDetectService.getState());
  }

  @Test
  void testSpecificStateGetters() {
    assertInstanceOf(ReadyState.class, debiasDetectService.getReady());
    assertInstanceOf(ProcessingState.class, debiasDetectService.getProcessing());
    assertInstanceOf(CompletedState.class, debiasDetectService.getCompleted());
    assertInstanceOf(ErrorState.class, debiasDetectService.getError());
  }

  @Test
  void testGetDetectionInfo_ObjectWhenExists_expectSuccess() {
    final Integer datasetId = 1;
    final String stateName = "READY";
    final ZonedDateTime createdDate = ZonedDateTime.now();
    DatasetEntity dataset = new DatasetEntity();
    dataset.setDatasetId(datasetId);
    DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState(stateName);
    detectionEntity.setDatasetId(dataset);
    detectionEntity.setCreatedDate(createdDate);
    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(datasetId))
        .thenReturn(detectionEntity);

    DetectionInfoDto detectionInfoDto = debiasDetectService.getDetectionInfo(datasetId);

    assertEquals(datasetId, detectionInfoDto.getDatasetId());
    assertEquals(stateName, detectionInfoDto.getState());
    assertEquals(createdDate, detectionInfoDto.getCreationDate());
  }

  @Test
  void testGetDetectionInfo_DefaultWhenNotExists_expectSuccess() {
    final Integer datasetId = 1;

    DetectionInfoDto detectionInfoDto = debiasDetectService.getDetectionInfo(datasetId);

    assertNotNull(detectionInfoDto);
    assertEquals(datasetId, detectionInfoDto.getDatasetId());
    assertEquals("READY", detectionInfoDto.getState());
  }

}
