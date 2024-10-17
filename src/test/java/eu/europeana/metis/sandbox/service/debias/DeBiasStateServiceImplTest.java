package eu.europeana.metis.sandbox.service.debias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.debias.DeBiasReportDto;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.RecordEntity.RecordEntityBuilder;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasDetailEntity;
import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasMainEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.debias.DatasetDeBiasRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasDetailRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasMainRepository;
import eu.europeana.metis.sandbox.service.workflow.DeBiasSourceField;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeBiasStateServiceImplTest {

  @Mock
  DatasetDeBiasRepository datasetDeBiasRepository;

  @Mock
  DatasetRepository datasetRepository;

  @Mock
  RecordLogRepository recordLogRepository;

  @Mock
  RecordDeBiasPublishable recordDeBiasPublishable;

  @Mock
  RecordDeBiasMainRepository recordDeBiasMainRepository;

  @Mock
  RecordDeBiasDetailRepository recordDeBiasDetailRepository;

  @InjectMocks
  DeBiasStateServiceImpl debiasStateServiceImpl;

  @Test
  void processWhenDatasetNotExists_expectSuccess() {
    final Integer datasetId = 1;
    final DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    datasetDeBiasEntity.setState("READY");
    datasetDeBiasEntity.setCreatedDate(ZonedDateTime.now());

    when(datasetRepository.findById(anyInt())).thenThrow(NoSuchElementException.class);

    boolean result = debiasStateServiceImpl.process(datasetId);

    assertFalse(result);

    verify(datasetDeBiasRepository, times(0)).findDetectionEntityByDatasetIdDatasetId(datasetId);
    verify(datasetDeBiasRepository, times(0)).save(any(DatasetDeBiasEntity.class));
    verify(datasetDeBiasRepository, times(0)).updateState(anyInt(), anyString());
    verify(recordLogRepository, times(0)).findRecordLogByDatasetIdAndStep(anyString(),any());
    verify(recordDeBiasPublishable, times(0)).publishToDeBiasQueue(any());
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
    final RecordLogEntity recordLogEntity = new RecordLogEntity();
    recordLogEntity.setId(1L);
    recordLogEntity.setRecordId(new RecordEntity("providerId","1"));
    recordLogEntity.setStep(Step.NORMALIZE);
    recordLogEntity.setContent("");
    recordLogEntity.setStatus(Status.SUCCESS);
    when(datasetRepository.findById(anyInt())).thenReturn(Optional.of(datasetEntity));
    when(recordLogRepository.findRecordLogByDatasetIdAndStep(anyString(),any())).thenReturn(Set.of(recordLogEntity));
    when(datasetDeBiasRepository.save(any(DatasetDeBiasEntity.class))).thenReturn(datasetDeBiasEntity);
    when(datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(anyInt()))
        .thenReturn(null)
        .thenReturn(datasetDeBiasEntity)
        .thenReturn(datasetDeBiasEntity);

    boolean result = debiasStateServiceImpl.process(datasetId);

    assertTrue(result);

    verify(datasetDeBiasRepository, times(1)).findDetectionEntityByDatasetIdDatasetId(datasetId);
    verify(datasetDeBiasRepository, times(1)).save(any(DatasetDeBiasEntity.class));
    verify(datasetDeBiasRepository, times(0)).updateState(anyInt(), anyString());
    verify(recordLogRepository, times(1)).findRecordLogByDatasetIdAndStep(anyString(),any());
    verify(recordDeBiasPublishable, times(1)).publishToDeBiasQueue(any());
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
    when(datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(anyInt()))
        .thenThrow(new RuntimeException("Error"));

    boolean result = debiasStateServiceImpl.process(datasetId);

    assertFalse(result);

    verify(datasetDeBiasRepository, times(1)).findDetectionEntityByDatasetIdDatasetId(datasetId);
    verify(datasetDeBiasRepository, times(0)).save(any(DatasetDeBiasEntity.class));
    verify(datasetDeBiasRepository, times(0)).updateState(anyInt(), anyString());
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
    when(datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(datasetId))
        .thenReturn(datasetDeBiasEntity);
    RecordDeBiasMainEntity recordDeBiasMainEntity = new RecordDeBiasMainEntity(
        new RecordEntityBuilder().setDatasetId(datasetId.toString()).build(),
        "literal", Language.NL, DeBiasSourceField.DC_DESCRIPTION);
    recordDeBiasMainEntity.setId(1L);
    RecordDeBiasDetailEntity recordDeBiasDetailEntity = new RecordDeBiasDetailEntity(recordDeBiasMainEntity,1,5,5,"uri");
    when(recordDeBiasMainRepository.findByRecordIdDatasetId(anyString())).thenReturn(List.of(recordDeBiasMainEntity));
    when(recordDeBiasDetailRepository.findByDebiasIdId(anyLong())).thenReturn(List.of(recordDeBiasDetailEntity));

    DeBiasReportDto deBiasReportDto = debiasStateServiceImpl.getDeBiasReport(datasetId);

    assertEquals(datasetId, deBiasReportDto.getDatasetId());
    assertEquals(stateName, deBiasReportDto.getState());
    assertEquals(createdDate, deBiasReportDto.getCreationDate());
  }

  @Test
  void testGetDetectionInfo_DefaultWhenNotExists_expectSuccess() {
    final Integer datasetId = 1;

    DeBiasReportDto deBiasReportDto = debiasStateServiceImpl.getDeBiasReport(datasetId);

    assertNotNull(deBiasReportDto);
    assertEquals(datasetId, deBiasReportDto.getDatasetId());
    assertEquals("READY", deBiasReportDto.getState());
  }
}
