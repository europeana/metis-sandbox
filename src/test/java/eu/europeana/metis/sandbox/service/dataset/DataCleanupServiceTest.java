package eu.europeana.metis.sandbox.service.dataset;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import eu.europeana.metis.sandbox.service.problempatterns.ProblemPatternDataRemover;
import eu.europeana.metis.sandbox.service.record.ExecutionRecordRemover;
import eu.europeana.metis.sandbox.service.util.ThumbnailStoreService;
import eu.europeana.metis.sandbox.service.workflow.IndexingService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataCleanupServiceTest {

  @Mock
  private DatasetService datasetService;

  @Mock
  private ExecutionRecordRemover executionRecordRemover;

  @Mock
  private IndexingService indexingService;

  @Mock
  private ThumbnailStoreService thumbnailStoreService;

  @Mock
  private ProblemPatternDataRemover problemPatternDataRemover;

  @Mock
  private HarvestingParameterService harvestingParameterService;

  @Mock
  private DeBiasStateService deBiasStateService;

  @InjectMocks
  private DataCleanupService dataCleanupService;

  @Test
  void remove_expectSuccess() {
    when(datasetService.getDatasetIdsCreatedBefore(7))
        .thenReturn(List.of("1", "2", "3", "4"));

    dataCleanupService.remove(7);

    verify(thumbnailStoreService, times(4)).remove(anyString());
    verify(indexingService, times(4)).remove(anyString());
    verify(harvestingParameterService, times(4)).remove(anyString());
    verify(executionRecordRemover, times(4)).remove(anyString());
    verify(datasetService, times(4)).remove(anyString());
    verify(problemPatternDataRemover, times(4)).remove(anyString());
    verify(deBiasStateService, times(4)).remove(anyInt());
  }

  @Test
  void remove_failToRemoveOneDataset_expectSuccess() {
    when(datasetService.getDatasetIdsCreatedBefore(7))
        .thenReturn(List.of("1", "2", "3", "4"));

    doThrow(new ServiceException("1", new Exception()))
        .when(thumbnailStoreService)
        .remove("1");

    dataCleanupService.remove(7);

    verify(thumbnailStoreService, times(4)).remove(anyString());
    verify(indexingService, times(3)).remove(anyString());
    verify(harvestingParameterService, times(3)).remove(anyString());
    verify(executionRecordRemover, times(3)).remove(anyString());
    verify(datasetService, times(3)).remove(anyString());
    verify(problemPatternDataRemover, times(3)).remove(anyString());
    verify(deBiasStateService, times(3)).remove(anyInt());
  }

  @Test
  void remove_failToRemoveThrowException_expectLogError() {
    doThrow(new ServiceException("Error getting ids", new RuntimeException()))
        .when(datasetService)
        .getDatasetIdsCreatedBefore(7);

    dataCleanupService.remove(7);

    verify(datasetService, times(1)).getDatasetIdsCreatedBefore(anyInt());
  }
}
