package eu.europeana.metis.sandbox.service.debias;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.debias.detect.model.response.Tag;
import eu.europeana.metis.debias.detect.model.response.ValueDetection;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.config.batch.DebiasJobConfig;
import eu.europeana.metis.sandbox.dto.debias.DeBiasReportDTO;
import eu.europeana.metis.sandbox.dto.debias.DeBiasStatusDTO;
import eu.europeana.metis.sandbox.dto.debias.DebiasState;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasDetailEntity;
import eu.europeana.metis.sandbox.entity.debias.RecordDeBiasMainEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.debias.DatasetDeBiasRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasDetailRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasMainRepository;
import eu.europeana.metis.sandbox.service.debias.DeBiasProcessService.DeBiasReportRow;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeBiasStateServiceTest {

  @Mock
  private DatasetDeBiasRepository datasetDeBiasRepository;
  @Mock
  private DatasetRepository datasetRepository;
  @Mock
  private RecordDeBiasMainRepository recordDeBiasMainRepository;
  @Mock
  private RecordDeBiasDetailRepository recordDeBiasDetailRepository;
  @Mock
  private ExecutionRecordRepository executionRecordRepository;
  @InjectMocks
  private DeBiasStateService deBiasStateService;

  @Test
  void testCreateDatasetDeBiasEntity_NewEntity() {
    String datasetId = "1";
    DatasetEntity datasetEntity = new DatasetEntity();
    when(datasetRepository.findById(Integer.valueOf(datasetId))).thenReturn(Optional.of(datasetEntity));
    when(datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(Integer.valueOf(datasetId))).thenReturn(null);

    ArgumentCaptor<DatasetDeBiasEntity> captor = ArgumentCaptor.forClass(DatasetDeBiasEntity.class);
    when(datasetDeBiasRepository.save(captor.capture()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    DatasetDeBiasEntity datasetDeBiasEntity = deBiasStateService.createDatasetDeBiasEntity(datasetId);

    assertNotNull(datasetDeBiasEntity);
    assertEquals(datasetEntity, datasetDeBiasEntity.getDatasetId());
    assertEquals(DebiasState.READY, datasetDeBiasEntity.getDebiasState());
  }

  @Test
  void testCreateDatasetDeBiasEntity_ExistingEntity() {
    String datasetId = "1";
    DatasetEntity datasetEntity = new DatasetEntity();
    when(datasetRepository.findById(Integer.valueOf(datasetId))).thenReturn(Optional.of(datasetEntity));
    DatasetDeBiasEntity storedDatasetDeBiasEntity = new DatasetDeBiasEntity();
    storedDatasetDeBiasEntity.setDebiasState(DebiasState.PROCESSING);
    ZonedDateTime nowDate = ZonedDateTime.now();
    storedDatasetDeBiasEntity.setCreatedDate(nowDate);
    storedDatasetDeBiasEntity.setDatasetId(datasetEntity);
    when(datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(Integer.valueOf(datasetId))).thenReturn(
        storedDatasetDeBiasEntity);

    DatasetDeBiasEntity datasetDeBiasEntity = deBiasStateService.createDatasetDeBiasEntity(datasetId);

    assertNotNull(datasetDeBiasEntity);
    assertEquals(datasetEntity, datasetDeBiasEntity.getDatasetId());
    assertEquals(DebiasState.PROCESSING, datasetDeBiasEntity.getDebiasState());
    assertEquals(nowDate, datasetDeBiasEntity.getCreatedDate());
  }

  @Test
  void testGetDeBiasStatus_Ready() {
    String datasetId = "1";
    when(executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        FullBatchJobType.VALIDATE_INTERNAL.name())).thenReturn(10L);
    when(executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        DebiasJobConfig.BATCH_JOB.name())).thenReturn(0L);

    DeBiasStatusDTO deBiasStatusDTO = deBiasStateService.getDeBiasStatus(datasetId);

    assertEquals(DebiasState.READY, deBiasStatusDTO.getDebiasState());
    assertEquals(10, deBiasStatusDTO.getTotal());
    assertEquals(0, deBiasStatusDTO.getProcessed());
  }

  @Test
  void testGetDeBiasStatus_Processing() {
    String datasetId = "1";
    DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    ZonedDateTime nowDate = ZonedDateTime.now();
    datasetDeBiasEntity.setCreatedDate(nowDate);
    when(datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(Integer.valueOf(datasetId))).thenReturn(
        datasetDeBiasEntity);
    when(executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        FullBatchJobType.VALIDATE_INTERNAL.name())).thenReturn(10L);
    when(executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        DebiasJobConfig.BATCH_JOB.name())).thenReturn(5L);

    DeBiasStatusDTO deBiasStatusDTO = deBiasStateService.getDeBiasStatus(datasetId);

    assertEquals(DebiasState.PROCESSING, deBiasStatusDTO.getDebiasState());
    assertEquals(nowDate, deBiasStatusDTO.getCreationDate());
    assertEquals(10, deBiasStatusDTO.getTotal());
    assertEquals(5, deBiasStatusDTO.getProcessed());
  }

  @Test
  void testGetDeBiasStatus_Completed() {
    String datasetId = "1";
    DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    ZonedDateTime nowDate = ZonedDateTime.now();
    datasetDeBiasEntity.setCreatedDate(nowDate);
    when(datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(Integer.valueOf(datasetId))).thenReturn(
        datasetDeBiasEntity);
    when(executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        FullBatchJobType.VALIDATE_INTERNAL.name())).thenReturn(10L);
    when(executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        DebiasJobConfig.BATCH_JOB.name())).thenReturn(10L);

    DeBiasStatusDTO deBiasStatusDTO = deBiasStateService.getDeBiasStatus(datasetId);

    assertEquals(DebiasState.COMPLETED, deBiasStatusDTO.getDebiasState());
    assertEquals(nowDate, deBiasStatusDTO.getCreationDate());
    assertEquals(10, deBiasStatusDTO.getTotal());
    assertEquals(10, deBiasStatusDTO.getProcessed());
  }

  @Test
  void testGetDeBiasStatus_Invalid() {
    String datasetId = "1";
    DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    ZonedDateTime nowDate = ZonedDateTime.now();
    datasetDeBiasEntity.setCreatedDate(nowDate);
    when(datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(Integer.valueOf(datasetId))).thenReturn(
        datasetDeBiasEntity);
    when(executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        FullBatchJobType.VALIDATE_INTERNAL.name())).thenReturn(-10L);
    when(executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        DebiasJobConfig.BATCH_JOB.name())).thenReturn(0L);

    DeBiasStatusDTO deBiasStatusDTO = deBiasStateService.getDeBiasStatus(datasetId);

    assertEquals(DebiasState.INVALID, deBiasStatusDTO.getDebiasState());
    assertEquals(nowDate, deBiasStatusDTO.getCreationDate());
    assertEquals(-10, deBiasStatusDTO.getTotal());
    assertEquals(0, deBiasStatusDTO.getProcessed());
  }

  @Test
  void getDeBiasReport() {
    String datasetId = "1";
    DatasetDeBiasEntity datasetDeBiasEntity = new DatasetDeBiasEntity();
    ZonedDateTime nowDate = ZonedDateTime.now();
    datasetDeBiasEntity.setCreatedDate(nowDate);
    when(datasetDeBiasRepository.findDetectionEntityByDatasetIdDatasetId(Integer.valueOf(datasetId))).thenReturn(
        datasetDeBiasEntity);
    when(executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        FullBatchJobType.VALIDATE_INTERNAL.name())).thenReturn(10L);
    when(executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        DebiasJobConfig.BATCH_JOB.name())).thenReturn(10L);

    // Report rows
    RecordDeBiasMainEntity recordDeBiasMainEntity = new RecordDeBiasMainEntity();
    recordDeBiasMainEntity.setId(1L);
    recordDeBiasMainEntity.setLiteral("literal");
    recordDeBiasMainEntity.setLanguage(Language.EL);
    recordDeBiasMainEntity.setRecordId("recordId");
    recordDeBiasMainEntity.setSourceField(DeBiasSourceField.DC_TITLE);

    RecordDeBiasDetailEntity recordDeBiasDetailEntity = new RecordDeBiasDetailEntity(recordDeBiasMainEntity, 0, 5, 5,
        "https://example.org/tag");

    when(recordDeBiasMainRepository.findByDatasetId_DatasetId(Integer.valueOf(datasetId))).thenReturn(
        List.of(recordDeBiasMainEntity));
    when(recordDeBiasDetailRepository.findByDebiasIdId(recordDeBiasMainEntity.getId())).thenReturn(
        List.of(recordDeBiasDetailEntity));

    DeBiasReportDTO deBiasReportDTO = deBiasStateService.getDeBiasReport(datasetId);

    assertEquals(DebiasState.COMPLETED, deBiasReportDTO.getDebiasState());
    assertEquals(nowDate, deBiasReportDTO.getCreationDate());
    assertEquals(10, deBiasReportDTO.getTotal());
    assertEquals(10, deBiasReportDTO.getProcessed());

    List<DeBiasReportRow> deBiasReportRowList = deBiasReportDTO.getDeBiasReportRowList();
    assertEquals(1, deBiasReportRowList.size());
    DeBiasReportRow deBiasReportRow = deBiasReportRowList.getFirst();
    assertEquals(recordDeBiasMainEntity.getRecordId(), deBiasReportRow.europeanaId());
    ValueDetection valueDetection = deBiasReportRow.valueDetection();
    assertEquals(recordDeBiasMainEntity.getLiteral(), valueDetection.getLiteral());
    assertEquals(recordDeBiasMainEntity.getLanguage().name().toLowerCase(Locale.US), valueDetection.getLanguage());
    List<Tag> tags = valueDetection.getTags();
    assertEquals(1, tags.size());
    Tag tag = tags.getFirst();
    assertEquals(recordDeBiasDetailEntity.getTagStart(), tag.getStart());
    assertEquals(recordDeBiasDetailEntity.getTagEnd(), tag.getEnd());
    assertEquals(recordDeBiasDetailEntity.getTagLength(), tag.getLength());
    assertEquals(recordDeBiasDetailEntity.getTagUri(), tag.getUri());
  }


  @Test
  void remove() {
    String datasetId = "1";

    assertDoesNotThrow(() -> deBiasStateService.remove(datasetId));

    verify(recordDeBiasDetailRepository).deleteByDatasetId(datasetId);
    verify(recordDeBiasMainRepository).deleteByDatasetId(datasetId);
    verify(datasetDeBiasRepository).deleteByDatasetId(datasetId);
  }

  @Test
  void remove_NullValues_Exceptions() {
    assertThrows(NullPointerException.class, () -> deBiasStateService.remove(null));
    assertThrows(IllegalArgumentException.class, () -> deBiasStateService.remove(""));
  }
}
