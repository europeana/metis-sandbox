package eu.europeana.metis.sandbox.service.debias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.dto.debias.DetectionInfoDto;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.debias.DetectionEntity;
import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DebiasMachineServiceTest {

  @Mock
  DetectRepository detectRepository;

  @InjectMocks
  DebiasMachineService debiasMachineService;


  @Test
  void processWhenNewHappyPath_Ready_Processing_Completed_expectSuccess() {
    final Long datasetId = 1L;
    final DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState("READY");
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId.intValue());
    detectionEntity.setDatasetId(datasetEntity);

    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyLong()))
        .thenReturn(null)
        .thenReturn(detectionEntity)
        .thenReturn(detectionEntity);

    boolean result = debiasMachineService.process(datasetId);

    assertTrue(result);
    assertInstanceOf(CompletedState.class, debiasMachineService.getState());
    verify(detectRepository, times(3)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(1)).save(any(DetectionEntity.class));
    verify(detectRepository, times(2)).updateState(anyLong(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_andError_expectSuccess() {
    final Long datasetId = 1L;
    final DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState("READY");
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId.intValue());
    detectionEntity.setDatasetId(datasetEntity);

    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyLong()))
        .thenThrow(new RuntimeException("Error"));

    boolean result = debiasMachineService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ReadyState.class, debiasMachineService.getState());
    verify(detectRepository, times(1)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(0)).save(any(DetectionEntity.class));
    verify(detectRepository, times(0)).updateState(anyLong(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_Processing_Completed_andError_expectSuccess() {
    final Long datasetId = 1L;
    final DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState("READY");
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId.intValue());
    detectionEntity.setDatasetId(datasetEntity);

    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyLong()))
        .thenReturn(null)
        .thenReturn(detectionEntity)
        .thenReturn(null)
        .thenReturn(null);


    boolean result = debiasMachineService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ErrorState.class, debiasMachineService.getState());
    verify(detectRepository, times(4)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(1)).save(any(DetectionEntity.class));
    verify(detectRepository, times(1)).updateState(anyLong(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_Processing_Completed_andException_expectSuccess() {
    final Long datasetId = 1L;
    final DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState("READY");
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId.intValue());
    detectionEntity.setDatasetId(datasetEntity);

    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyLong()))
        .thenReturn(null)
        .thenReturn(detectionEntity)
        .thenThrow(new RuntimeException("Error"))
        .thenReturn(null);

    boolean result = debiasMachineService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ErrorState.class, debiasMachineService.getState());
    verify(detectRepository, times(4)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(1)).save(any(DetectionEntity.class));
    verify(detectRepository, times(1)).updateState(anyLong(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_Processing_Error_Ready_Processing_Completed_expectSuccess() {
    final Long datasetId = 1L;
    final DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState("READY");
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId.intValue());
    detectionEntity.setDatasetId(datasetEntity);

    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyLong()))
        .thenReturn(null)
        .thenThrow(new RuntimeException("Error"))
        .thenReturn(detectionEntity)
        .thenReturn(detectionEntity)
        .thenReturn(detectionEntity)
        .thenReturn(detectionEntity);

    boolean result = debiasMachineService.process(datasetId);

    assertTrue(result);
    assertInstanceOf(CompletedState.class, debiasMachineService.getState());
    verify(detectRepository, times(6)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(1)).save(any(DetectionEntity.class));
    verify(detectRepository, times(4)).updateState(anyLong(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_Processing_andError_expectSuccess() {
    final Long datasetId = 1L;
    final DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState("READY");
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId.intValue());
    detectionEntity.setDatasetId(datasetEntity);

    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyLong()))
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null);

    boolean result = debiasMachineService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ErrorState.class, debiasMachineService.getState());
    verify(detectRepository, times(1)).save(any(DetectionEntity.class));
    verify(detectRepository, times(3)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(0)).updateState(anyLong(), anyString());
  }

  @Test
  void processWhenDatasetAlreadyExists_Ready_Processing_Completed_expectSuccess() {
    final Long datasetId = 1L;
    final String stateName = "READY";
    final DatasetEntity dataset = new DatasetEntity();
    dataset.setDatasetId(1);
    final DetectionEntity detectionEntity = new DetectionEntity(dataset, stateName);
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyLong()))
        .thenReturn(detectionEntity)
        .thenReturn(detectionEntity);

    boolean result = debiasMachineService.process(datasetId);

    assertTrue(result);
    assertInstanceOf(CompletedState.class, debiasMachineService.getState());
    verify(detectRepository, times(3)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(3)).updateState(anyLong(), anyString());
  }

  @Test
  void processWhenDatasetAlreadyExists_Ready_Processing_andError_expectSuccess() {
    final Long datasetId = 1L;
    final String stateName = "READY";
    final DatasetEntity dataset = new DatasetEntity();
    dataset.setDatasetId(datasetId.intValue());
    final DetectionEntity detectionEntity = new DetectionEntity(dataset, stateName);
    detectionEntity.setCreatedDate(ZonedDateTime.now());
    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(anyLong()))
        .thenReturn(detectionEntity)
        .thenReturn(null)
        .thenReturn(null);
    boolean result = debiasMachineService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ErrorState.class, debiasMachineService.getState());
    verify(detectRepository, times(3)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(detectRepository, times(1)).updateState(anyLong(), anyString());
  }

  @Test
  void set_and_get_State() {
    debiasMachineService.setState(new ReadyState(debiasMachineService, detectRepository));
    assertInstanceOf(ReadyState.class, debiasMachineService.getState());

    debiasMachineService.setState(new ProcessingState(debiasMachineService, detectRepository));
    assertInstanceOf(ProcessingState.class, debiasMachineService.getState());

    debiasMachineService.setState(new CompletedState(debiasMachineService, detectRepository));
    assertInstanceOf(CompletedState.class, debiasMachineService.getState());

    debiasMachineService.setState(new ErrorState(debiasMachineService, detectRepository));
    assertInstanceOf(ErrorState.class, debiasMachineService.getState());
  }

  @Test
  void testSpecificStateGetters() {
    assertInstanceOf(ReadyState.class, debiasMachineService.getReady());
    assertInstanceOf(ProcessingState.class, debiasMachineService.getProcessing());
    assertInstanceOf(CompletedState.class, debiasMachineService.getCompleted());
    assertInstanceOf(ErrorState.class, debiasMachineService.getError());
  }

  @Test
  void testGetDetectionInfo_ObjectWhenExists_expectSuccess() {
    final Long datasetId = 1L;
    final String stateName = "READY";
    final ZonedDateTime createdDate = ZonedDateTime.now();
    DatasetEntity dataset = new DatasetEntity();
    dataset.setDatasetId(datasetId.intValue());
    DetectionEntity detectionEntity = new DetectionEntity();
    detectionEntity.setState(stateName);
    detectionEntity.setDatasetId(dataset);
    detectionEntity.setCreatedDate(createdDate);
    when(detectRepository.findDetectionEntityByDatasetId_DatasetId(datasetId))
        .thenReturn(detectionEntity);

    DetectionInfoDto detectionInfoDto = debiasMachineService.getDetectionInfo(datasetId);

    assertEquals(datasetId, detectionInfoDto.getDatasetId());
    assertEquals(stateName, detectionInfoDto.getState());
    assertEquals(createdDate, detectionInfoDto.getCreationDate());
  }

  @Test
  void testGetDetectionInfo_NullWhenNotExists_expectSuccess() {
    Long datasetId = 1L;

    DetectionInfoDto detectionInfoDto = debiasMachineService.getDetectionInfo(datasetId);

    assertNull(detectionInfoDto);
  }

}
