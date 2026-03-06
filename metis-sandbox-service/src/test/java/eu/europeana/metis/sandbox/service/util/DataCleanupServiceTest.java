package eu.europeana.metis.sandbox.service.util;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.service.dataset.DatasetDataCleaner;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import eu.europeana.metis.sandbox.service.problempatterns.ProblemPatternDataCleaner;
import eu.europeana.metis.sandbox.service.record.ExecutionRecordCleaner;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataCleanupServiceTest {

  @Mock
  private DatasetDataCleaner datasetDataCleaner;

  @Mock
  private DatasetReportService datasetReportService;

  @Mock
  private ExecutionRecordCleaner executionRecordCleaner;

  @Mock
  private IndexDataCleaner indexDataCleaner;

  @Mock
  private ThumbnailStoreService thumbnailStoreService;

  @Mock
  private ProblemPatternDataCleaner problemPatternDataCleaner;

  @Mock
  private HarvestParameterService harvestParameterService;

  @Mock
  private DeBiasStateService deBiasStateService;

  @InjectMocks
  private DataCleanupService dataCleanupService;

  @Test
  void remove_expectSuccess() {
    when(datasetReportService.findDatasetIdsByCreatedBefore(7))
        .thenReturn(List.of("1", "2", "3", "4"));

    dataCleanupService.remove(7);

    verify(thumbnailStoreService, times(4)).remove(anyString());
    verify(indexDataCleaner, times(4)).remove(anyString());
    verify(harvestParameterService, times(4)).remove(anyString());
    verify(executionRecordCleaner, times(4)).remove(anyString());
    verify(datasetDataCleaner, times(4)).remove(anyString());
    verify(problemPatternDataCleaner, times(4)).remove(anyString());
    verify(deBiasStateService, times(4)).remove(anyString());
  }

  @Test
  void remove_failToRemoveOneDataset_expectSuccess() {
    when(datasetReportService.findDatasetIdsByCreatedBefore(7))
        .thenReturn(List.of("1", "2", "3", "4"));

    doThrow(new ServiceException("1", new Exception()))
        .when(thumbnailStoreService)
        .remove("1");

    dataCleanupService.remove(7);

    verify(thumbnailStoreService, times(4)).remove(anyString());
    verify(indexDataCleaner, times(3)).remove(anyString());
    verify(harvestParameterService, times(3)).remove(anyString());
    verify(executionRecordCleaner, times(3)).remove(anyString());
    verify(datasetDataCleaner, times(3)).remove(anyString());
    verify(problemPatternDataCleaner, times(3)).remove(anyString());
    verify(deBiasStateService, times(3)).remove(anyString());
  }

  @Test
  void remove_failToRemoveThrowException_expectLogError() {
    doThrow(new ServiceException("Error getting ids", new RuntimeException()))
        .when(datasetReportService)
        .findDatasetIdsByCreatedBefore(7);

    dataCleanupService.remove(7);

    verify(datasetReportService, times(1)).findDatasetIdsByCreatedBefore(anyInt());
  }
}
