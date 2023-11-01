package eu.europeana.metis.sandbox.service.dataset;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.service.problempatterns.ProblemPatternDataRemover;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.record.RecordService;
import eu.europeana.metis.sandbox.service.util.ThumbnailStoreService;
import eu.europeana.metis.sandbox.service.workflow.IndexingService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetRemoverServiceImplTest {

  @Mock
  private DatasetService datasetService;

  @Mock
  private DatasetLogService datasetLogService;

  @Mock
  private RecordLogService recordLogService;

  @Mock
  private IndexingService indexingService;

  @Mock
  private ThumbnailStoreService thumbnailStoreService;

  @Mock
  private RecordService recordService;

  @Mock
  private ProblemPatternDataRemover problemPatternDataRemover;

  @Mock
  private HarvestingParametersService harvestingParametersService;

  @InjectMocks
  private DatasetRemoverServiceImpl service;

  @Test
  void remove_expectSuccess() {
    when(datasetService.getDatasetIdsCreatedBefore(7))
        .thenReturn(List.of("1", "2", "3", "4"));

    service.remove(7);

    verify(thumbnailStoreService, times(4)).remove(anyString());
    verify(indexingService, times(4)).remove(anyString());
    verify(recordLogService, times(4)).remove(anyString());
    verify(harvestingParametersService, times(4)).remove(anyString());
    verify(datasetService, times(4)).remove(anyString());
    verify(datasetLogService, times(4)).remove(anyString());
    verify(recordService, times(4)).remove(anyString());
    verify(problemPatternDataRemover, times(4)).removeProblemPatternDataFromDatasetId(anyString());
  }

  @Test
  void remove_failToRemoveFirstDataset_expectSuccess() {
    when(datasetService.getDatasetIdsCreatedBefore(7))
        .thenReturn(List.of("1", "2", "3", "4"));

    doThrow(new ServiceException("1", new Exception()))
        .when(thumbnailStoreService)
        .remove("1");

    service.remove(7);

    verify(thumbnailStoreService, times(4)).remove(anyString());
    verify(indexingService, times(3)).remove(anyString());
    verify(recordLogService, times(3)).remove(anyString());
    verify(harvestingParametersService, times(3)).remove(anyString());
    verify(datasetService, times(3)).remove(anyString());
    verify(datasetLogService, times(3)).remove(anyString());
    verify(recordService, times(3)).remove(anyString());
    verify(problemPatternDataRemover, times(3)).removeProblemPatternDataFromDatasetId(anyString());
  }

  @Test
  void remove_failToRemoveLastDataset_expectSuccess() {
    when(datasetService.getDatasetIdsCreatedBefore(7))
        .thenReturn(List.of("1", "2", "3", "4"));

    doThrow(new ServiceException("4", new Exception()))
        .when(thumbnailStoreService)
        .remove("1");

    service.remove(7);

    verify(thumbnailStoreService, times(4)).remove(anyString());
    verify(indexingService, times(3)).remove(anyString());
    verify(recordLogService, times(3)).remove(anyString());
    verify(harvestingParametersService, times(3)).remove(anyString());
    verify(datasetService, times(3)).remove(anyString());
    verify(datasetLogService, times(3)).remove(anyString());
    verify(recordService, times(3)).remove(anyString());
    verify(problemPatternDataRemover, times(3)).removeProblemPatternDataFromDatasetId(anyString());
  }

  @Test
  void remove_failToRemoveThrowException_expectLogError() {
    doThrow(new ServiceException("Error getting ids", new RuntimeException()))
        .when(datasetService)
        .getDatasetIdsCreatedBefore(7);

    service.remove(7);

    verify(datasetService, times(1)).getDatasetIdsCreatedBefore(anyInt());
  }
}
