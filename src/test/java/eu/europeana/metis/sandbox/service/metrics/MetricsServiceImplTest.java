package eu.europeana.metis.sandbox.service.metrics;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.metrics.ProgressDatasetEntity;
import eu.europeana.metis.sandbox.entity.metrics.ProgressStepEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.metrics.ProgressDatasetRepository;
import eu.europeana.metis.sandbox.repository.metrics.ProgressStepRepository;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsServiceImplTest {

  @Mock
  DatasetRepository datasetRepository;
  @Mock
  DatasetReportService datasetReportService;
  @Mock
  ProgressDatasetRepository progressDatasetRepository;
  @Mock
  ProgressStepRepository progressStepRepository;
  @InjectMocks
  private MetricsServiceImpl metricsService;

  @Test
  void datasetMetrics() {
    when(progressDatasetRepository.findAll()).thenReturn(getProgressDatasetRepository());
    when(progressStepRepository.findAll()).thenReturn(getProgressStepRepository());

    DatasetMetrics datasetMetrics = metricsService.datasetMetrics();

    verifyMetrics(datasetMetrics);
    verify(progressDatasetRepository).findAll();
    verify(progressStepRepository).findAll();
  }

  @Test
  void resetDatasetMetrics() {
    metricsService.resetDatasetMetrics();

    verify(progressStepRepository).deleteAll();
    verify(progressDatasetRepository).deleteAll();
  }

  @Test
  void processMetrics_NewDataset_expectSuccess() {
    when(datasetRepository.findById(any())).thenReturn(getDatasetEntity());
    when(datasetReportService.getReport(anyString())).thenReturn(getProgressInfoDto());

    metricsService.processMetrics("1");

    verify(datasetRepository).findById(any());
    verify(datasetReportService).getReport(anyString());
    verify(progressDatasetRepository).save(any(ProgressDatasetEntity.class));
    verify(progressStepRepository, times(8)).save(any(ProgressStepEntity.class));
  }

  @Test
  void processMetrics_ExistingDataset_expectSuccess() {
    when(progressStepRepository.findByDatasetIdAndStep(anyString(), anyString()))
        .thenReturn(getProgressStepRepository().get(0))
        .thenReturn(getProgressStepRepository().get(1))
        .thenReturn(getProgressStepRepository().get(2))
        .thenReturn(getProgressStepRepository().get(3))
        .thenReturn(getProgressStepRepository().get(4))
        .thenReturn(getProgressStepRepository().get(5))
        .thenReturn(getProgressStepRepository().get(6))
        .thenReturn(getProgressStepRepository().get(7));
    when(progressDatasetRepository.findByDatasetId(anyString())).thenReturn(getProgressDataset());
    when(datasetReportService.getReport(anyString())).thenReturn(getProgressInfoDto());

    metricsService.processMetrics("1");

    verify(datasetRepository, never()).findById(any());
    verify(progressDatasetRepository).save(any(ProgressDatasetEntity.class));
    verify(progressStepRepository, times(8)).findByDatasetIdAndStep(anyString(), anyString());
    verify(progressStepRepository, times(8)).save(any(ProgressStepEntity.class));
  }

  @Test
  void processMetrics_exception_expectFailure() {
    doThrow(new RuntimeException("Exception fetching information")).when(datasetRepository).findById(any());
    when(datasetReportService.getReport(anyString())).thenReturn(getProgressInfoDto());

    metricsService.processMetrics("1");

    verify(datasetReportService).getReport(anyString());
    verify(progressDatasetRepository, never()).save(any(ProgressDatasetEntity.class));
    verify(progressStepRepository, never()).findByDatasetIdAndStep(anyString(), anyString());
    verify(progressStepRepository, never()).save(any(ProgressStepEntity.class));
  }

  private void verifyMetrics(DatasetMetrics datasetMetrics) {
    assertTrue(datasetMetrics.getDatasetMetricsMap().containsKey("MetricsByDataset"));
    assertTrue(datasetMetrics.getDatasetMetricsMap().containsKey("MetricsByStep"));
    assertTrue(((Map<String, Object>) datasetMetrics.getDatasetMetricsMap().get("MetricsByDataset")).containsKey("ProcessedRecords"));
    assertTrue(((Map<String, Object>) datasetMetrics.getDatasetMetricsMap().get("MetricsByDataset")).containsKey("DatasetCount"));
    assertTrue(((Map<String, Object>) datasetMetrics.getDatasetMetricsMap().get("MetricsByDataset")).containsKey("TotalRecords"));
    assertTrue(((Map<String, Object>) datasetMetrics.getDatasetMetricsMap().get("MetricsByDataset")).containsKey("Duration"));

    assertTrue(((Map<String, Object>) datasetMetrics.getDatasetMetricsMap().get("MetricsByStep")).containsKey(Step.HARVEST_ZIP.value()));
    assertTrue(((Map<String, Object>) datasetMetrics.getDatasetMetricsMap().get("MetricsByStep")).containsKey(Step.VALIDATE_EXTERNAL.value()));
    assertTrue(((Map<String, Object>) datasetMetrics.getDatasetMetricsMap().get("MetricsByStep")).containsKey(Step.TRANSFORM.value()));
    assertTrue(((Map<String, Object>) datasetMetrics.getDatasetMetricsMap().get("MetricsByStep")).containsKey(Step.HARVEST_OAI_PMH.value()));
    assertTrue(((Map<String, Object>) datasetMetrics.getDatasetMetricsMap().get("MetricsByStep")).containsKey(Step.VALIDATE_INTERNAL.value()));
    assertTrue(((Map<String, Object>) datasetMetrics.getDatasetMetricsMap().get("MetricsByStep")).containsKey(Step.NORMALIZE.value()));
    assertTrue(((Map<String, Object>) datasetMetrics.getDatasetMetricsMap().get("MetricsByStep")).containsKey(Step.ENRICH.value()));
    assertTrue(((Map<String, Object>) datasetMetrics.getDatasetMetricsMap().get("MetricsByStep")).containsKey(Step.MEDIA_PROCESS.value()));
    assertTrue(((Map<String, Object>) datasetMetrics.getDatasetMetricsMap().get("MetricsByStep")).containsKey(Step.PUBLISH.value()));
  }

  private List<ProgressDatasetEntity> getProgressDatasetRepository() {
    return List.of(getProgressDataset());
  }

  @NotNull
  private ProgressDatasetEntity getProgressDataset() {
    ProgressDatasetEntity progressDatasetEntity = new ProgressDatasetEntity();
    progressDatasetEntity.setDatasetId("1");
    progressDatasetEntity.setProcessedRecords(40L);
    progressDatasetEntity.setTotalRecords(100L);
    progressDatasetEntity.setStatus("in progress");
    progressDatasetEntity.setStartTimeStamp(LocalDateTime.now().minusSeconds(10));
    progressDatasetEntity.setEndTimeStamp(LocalDateTime.now().minusSeconds(10));
    return progressDatasetEntity;
  }

  @NotNull
  private Optional<DatasetEntity> getDatasetEntity() {
    return Optional.of(new DatasetEntity("dataset", 100L, Language.NL, Country.NETHERLANDS, false, ""));
  }

  @NotNull
  private List<ProgressStepEntity> getProgressStepRepository() {
    return List.of(
        new ProgressStepEntity() {{
          setStep(Step.HARVEST_ZIP.value()); setSuccess(100L); setTotal(100L);
          setWarn(0L); setFail(0L); setDatasetId("1");
        }},
        new ProgressStepEntity() {{
          setStep(Step.VALIDATE_EXTERNAL.value()); setSuccess(100L); setTotal(100L);
          setWarn(0L); setFail(0L); setDatasetId("1");
        }},
        new ProgressStepEntity() {{
          setStep(Step.TRANSFORM.value()); setSuccess(100L); setTotal(100L);
          setWarn(0L); setFail(0L); setDatasetId("1");
        }},
        new ProgressStepEntity() {{
          setStep(Step.VALIDATE_INTERNAL.value()); setSuccess(100L); setTotal(100L);
          setWarn(0L); setFail(0L); setDatasetId("1");
        }},
        new ProgressStepEntity() {{
          setStep(Step.NORMALIZE.value()); setSuccess(100L); setTotal(100L);
          setWarn(0L); setFail(0L); setDatasetId("1");
        }},
        new ProgressStepEntity() {{
          setStep(Step.ENRICH.value()); setSuccess(100L); setTotal(100L);
          setWarn(0L); setFail(0L); setDatasetId("1");
        }},
        new ProgressStepEntity() {{
          setStep(Step.MEDIA_PROCESS.value()); setSuccess(100L); setTotal(100L);
          setWarn(0L); setFail(0L); setDatasetId("1");
        }},
        new ProgressStepEntity() {{
          setStep(Step.PUBLISH.value()); setSuccess(100L); setTotal(100L);
          setWarn(0L); setFail(0L); setDatasetId("1");
        }}
    );
  }

  @NotNull
  private ProgressInfoDto getProgressInfoDto() {
    List<ProgressByStepDto> progressByStepDtos = List.of(
        new ProgressByStepDto(Step.HARVEST_ZIP, 100, 0, 0, List.of()),
        new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 100, 0, 5,
            List.of(new ErrorInfoDto("warn1", Status.WARN, List.of("r1", "r2", "r3", "r4", "r5")))),
        new ProgressByStepDto(Step.TRANSFORM, 95, 5, 0,
            List.of(new ErrorInfoDto("fail1", Status.FAIL, List.of("r1", "r2", "r3", "r4", "r5")))),
        new ProgressByStepDto(Step.VALIDATE_INTERNAL, 95, 0, 0,
            List.of()),
        new ProgressByStepDto(Step.NORMALIZE, 95, 0, 0,
            List.of()),
        new ProgressByStepDto(Step.ENRICH, 95, 0, 0,
            List.of()),
        new ProgressByStepDto(Step.MEDIA_PROCESS, 95, 0, 5,
            List.of(new ErrorInfoDto("warn1", Status.WARN, List.of("r1", "r2", "r3", "r4", "r5")))),
        new ProgressByStepDto(Step.PUBLISH, 95, 0, 0,
            List.of())
    );
    DatasetInfoDto datasetInfoDto = new DatasetInfoDto("1", "datasetName", LocalDateTime.now(),
        Language.NL, Country.NETHERLANDS, false, false);
    ProgressInfoDto progressInfoDto = new ProgressInfoDto("http://portal",
        100L, 100L, progressByStepDtos, datasetInfoDto, "");
    return progressInfoDto;
  }
}
