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
import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.debias.DatasetDeBiasRepository;
import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeBiasStateServiceTest {

  @Mock
  DatasetDeBiasRepository datasetDeBiasRepository;

  @Mock
  DatasetRepository datasetRepository;

  @Mock
  RecordLogRepository recordLogRepository;

  @Mock
  RecordDeBiasPublishable recordDeBiasPublishable;

  @InjectMocks
  DeBiasStateService debiasStateService;

  @Test
  void processWhenDatasetNotExists_expectSuccess() {
    final Integer datasetId = 1;
    final DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    datasetDeBiasEntity.setState("READY");
    datasetDeBiasEntity.setCreatedDate(ZonedDateTime.now());

    when(datasetRepository.findById(anyInt())).thenThrow(NoSuchElementException.class);

    boolean result = debiasStateService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ReadyState.class, debiasStateService.getState());
    verify(datasetDeBiasRepository, times(0)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(datasetDeBiasRepository, times(0)).save(any(DatasetDeBiasEntity.class));
    verify(datasetDeBiasRepository, times(0)).updateState(anyInt(), anyString());
  }


  @Test
  void processWhenNewHappyPath_Ready_Processing_Completed_expectSuccess() {
    final Integer datasetId = 1;
    final DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    datasetDeBiasEntity.setState("READY");
    datasetDeBiasEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId);
    datasetDeBiasEntity.setDatasetId(datasetEntity);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    when(datasetDeBiasRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenReturn(null)
        .thenReturn(datasetDeBiasEntity)
        .thenReturn(datasetDeBiasEntity);

    boolean result = debiasStateService.process(datasetId);

    assertTrue(result);
    assertInstanceOf(CompletedState.class, debiasStateService.getState());
    verify(datasetDeBiasRepository, times(3)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(datasetDeBiasRepository, times(1)).save(any(DatasetDeBiasEntity.class));
    verify(datasetDeBiasRepository, times(2)).updateState(anyInt(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_andError_expectSuccess() {
    final Integer datasetId = 1;
    final DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    datasetDeBiasEntity.setState("READY");
    datasetDeBiasEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId);
    datasetDeBiasEntity.setDatasetId(datasetEntity);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    when(datasetDeBiasRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenThrow(new RuntimeException("Error"));

    boolean result = debiasStateService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ReadyState.class, debiasStateService.getState());
    verify(datasetDeBiasRepository, times(1)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(datasetDeBiasRepository, times(0)).save(any(DatasetDeBiasEntity.class));
    verify(datasetDeBiasRepository, times(0)).updateState(anyInt(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_Processing_Completed_andError_expectSuccess() {
    final Integer datasetId = 1;
    final DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    datasetDeBiasEntity.setState("READY");
    datasetDeBiasEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId);
    datasetDeBiasEntity.setDatasetId(datasetEntity);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    when(datasetDeBiasRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenReturn(null)
        .thenReturn(datasetDeBiasEntity)
        .thenReturn(null)
        .thenReturn(null);

    boolean result = debiasStateService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ErrorState.class, debiasStateService.getState());
    verify(datasetDeBiasRepository, times(4)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(datasetDeBiasRepository, times(1)).save(any(DatasetDeBiasEntity.class));
    verify(datasetDeBiasRepository, times(1)).updateState(anyInt(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_Processing_Completed_andException_expectSuccess() {
    final Integer datasetId = 1;
    final DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    datasetDeBiasEntity.setState("READY");
    datasetDeBiasEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId);
    datasetDeBiasEntity.setDatasetId(datasetEntity);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    when(datasetDeBiasRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenReturn(null)
        .thenReturn(datasetDeBiasEntity)
        .thenThrow(new RuntimeException("Error"))
        .thenReturn(null);

    boolean result = debiasStateService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ErrorState.class, debiasStateService.getState());
    verify(datasetDeBiasRepository, times(4)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(datasetDeBiasRepository, times(1)).save(any(DatasetDeBiasEntity.class));
    verify(datasetDeBiasRepository, times(1)).updateState(anyInt(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_Processing_Error_Ready_Processing_Completed_expectSuccess() {
    final Integer datasetId = 1;
    final DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    datasetDeBiasEntity.setState("READY");
    datasetDeBiasEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId);
    datasetDeBiasEntity.setDatasetId(datasetEntity);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    when(datasetDeBiasRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenReturn(null)
        .thenThrow(new RuntimeException("Error"))
        .thenReturn(datasetDeBiasEntity)
        .thenReturn(datasetDeBiasEntity)
        .thenReturn(datasetDeBiasEntity)
        .thenReturn(datasetDeBiasEntity);

    boolean result = debiasStateService.process(datasetId);

    assertTrue(result);
    assertInstanceOf(CompletedState.class, debiasStateService.getState());
    verify(datasetDeBiasRepository, times(6)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(datasetDeBiasRepository, times(1)).save(any(DatasetDeBiasEntity.class));
    verify(datasetDeBiasRepository, times(4)).updateState(anyInt(), anyString());
  }

  @Test
  void processWhenNewHappyPath_Ready_Processing_andError_expectSuccess() {
    final Integer datasetId = 1;
    final DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    datasetDeBiasEntity.setState("READY");
    datasetDeBiasEntity.setCreatedDate(ZonedDateTime.now());
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(datasetId);
    datasetDeBiasEntity.setDatasetId(datasetEntity);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    when(datasetDeBiasRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null);

    boolean result = debiasStateService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ErrorState.class, debiasStateService.getState());
    verify(datasetDeBiasRepository, times(1)).save(any(DatasetDeBiasEntity.class));
    verify(datasetDeBiasRepository, times(3)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(datasetDeBiasRepository, times(0)).updateState(anyInt(), anyString());
  }

  @Test
  void processWhenDatasetAlreadyExists_Ready_Processing_Completed_expectSuccess() {
    final Integer datasetId = 1;
    final String stateName = "READY";
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(1);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    final DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity(datasetEntity, stateName);
    datasetDeBiasEntity.setCreatedDate(ZonedDateTime.now());
    when(datasetDeBiasRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenReturn(datasetDeBiasEntity)
        .thenReturn(datasetDeBiasEntity);

    boolean result = debiasStateService.process(datasetId);

    assertTrue(result);
    assertInstanceOf(CompletedState.class, debiasStateService.getState());
    verify(datasetDeBiasRepository, times(3)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(datasetDeBiasRepository, times(3)).updateState(anyInt(), anyString());
  }

  @Test
  void processWhenDatasetAlreadyExists_Ready_Processing_andError_expectSuccess() {
    final Integer datasetId = 1;
    final String stateName = "READY";
    final DatasetEntity datasetEntity = new DatasetEntity();
    datasetEntity.setDatasetId(1);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    final DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity(datasetEntity, stateName);
    datasetDeBiasEntity.setCreatedDate(ZonedDateTime.now());
    when(datasetDeBiasRepository.findDetectionEntityByDatasetId_DatasetId(anyInt()))
        .thenReturn(datasetDeBiasEntity)
        .thenReturn(null)
        .thenReturn(null);
    boolean result = debiasStateService.process(datasetId);

    assertFalse(result);
    assertInstanceOf(ErrorState.class, debiasStateService.getState());

    verify(datasetDeBiasRepository, times(3)).findDetectionEntityByDatasetId_DatasetId(datasetId);
    verify(datasetDeBiasRepository, times(1)).updateState(anyInt(), anyString());
  }

  @Test
  void set_and_get_State() {
    debiasStateService.setState(new ReadyState(debiasStateService, datasetDeBiasRepository, datasetRepository, recordLogRepository, recordDeBiasPublishable));
    assertInstanceOf(ReadyState.class, debiasStateService.getState());

    debiasStateService.setState(new ProcessingState(debiasStateService, datasetDeBiasRepository));
    assertInstanceOf(ProcessingState.class, debiasStateService.getState());

    debiasStateService.setState(new CompletedState(debiasStateService, datasetDeBiasRepository));
    assertInstanceOf(CompletedState.class, debiasStateService.getState());

    debiasStateService.setState(new ErrorState(debiasStateService, datasetDeBiasRepository));
    assertInstanceOf(ErrorState.class, debiasStateService.getState());
  }

  @Test
  void testSpecificStateGetters() {
    assertInstanceOf(ReadyState.class, debiasStateService.getReady());
    assertInstanceOf(ProcessingState.class, debiasStateService.getProcessing());
    assertInstanceOf(CompletedState.class, debiasStateService.getCompleted());
    assertInstanceOf(ErrorState.class, debiasStateService.getError());
  }

  @Test
  void testGetDetectionInfo_ObjectWhenExists_expectSuccess() {
    final Integer datasetId = 1;
    final String stateName = "READY";
    final ZonedDateTime createdDate = ZonedDateTime.now();
    DatasetEntity dataset = new DatasetEntity();
    dataset.setDatasetId(datasetId);
    DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    datasetDeBiasEntity.setState(stateName);
    datasetDeBiasEntity.setDatasetId(dataset);
    datasetDeBiasEntity.setCreatedDate(createdDate);
    when(datasetDeBiasRepository.findDetectionEntityByDatasetId_DatasetId(datasetId))
        .thenReturn(datasetDeBiasEntity);

    DetectionInfoDto detectionInfoDto = debiasStateService.getDetectionInfo(datasetId);

    assertEquals(datasetId, detectionInfoDto.getDatasetId());
    assertEquals(stateName, detectionInfoDto.getState());
    assertEquals(createdDate, detectionInfoDto.getCreationDate());
  }

  @Test
  void testGetDetectionInfo_DefaultWhenNotExists_expectSuccess() {
    final Integer datasetId = 1;

    DetectionInfoDto detectionInfoDto = debiasStateService.getDetectionInfo(datasetId);

    assertNotNull(detectionInfoDto);
    assertEquals(datasetId, detectionInfoDto.getDatasetId());
    assertEquals("READY", detectionInfoDto.getState());
  }

}
