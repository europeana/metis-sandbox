package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.Integer.valueOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.DatasetRepository.DatasetIdProjection;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetServiceTest {

  @Mock
  private DatasetRepository datasetRepository;

  @InjectMocks
  private DatasetService datasetService;

  @Test
  void remove_shouldDeleteDataset() {
    String datasetId = "123";
    datasetService.remove(datasetId);
    verify(datasetRepository).deleteById(valueOf(datasetId));
  }

  @Test
  void remove_shouldThrowServiceExceptionWhenRepositoryThrows() {
    String datasetId = "123";
    doThrow(new RuntimeException()).when(datasetRepository).deleteById(valueOf(datasetId));
    assertThrows(ServiceException.class, () -> datasetService.remove(datasetId));
  }

  @Test
  void findDatasetIdsByCreatedBefore_shouldReturnDatasetIds() {
    int days = 10;
    ZonedDateTime expectedDate = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(days);

    List<String> datasetIds = List.of("111", "222");
    List<DatasetIdProjection> datasetIdProjections =
        datasetIds.stream()
                  .map(datasetId -> (DatasetIdProjection) () -> valueOf(datasetId))
                  .toList();

    when(datasetRepository.findByCreatedDateBefore(any(ZonedDateTime.class))).thenReturn(datasetIdProjections);
    List<String> result = datasetService.findDatasetIdsByCreatedBefore(days);
    assertEquals(datasetIds, result);

    ArgumentCaptor<ZonedDateTime> zonedDateTimeArgumentCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
    verify(datasetRepository, times(1)).findByCreatedDateBefore(zonedDateTimeArgumentCaptor.capture());
    ZonedDateTime actualArgument = zonedDateTimeArgumentCaptor.getValue();

    // Allow a small tolerance window because ZonedDateTime.now() is evaluated per line
    long secondsDifference = Math.abs(Duration.between(expectedDate, actualArgument).getSeconds());
    assertTrue(secondsDifference < 10);
  }

  @Test
  void findDatasetIdsByCreatedBefore_shouldThrowServiceExceptionWhenRepositoryThrows() {
    int days = 5;
    when(datasetRepository.findByCreatedDateBefore(any(ZonedDateTime.class))).thenThrow(new RuntimeException());
    assertThrows(ServiceException.class, () -> datasetService.findDatasetIdsByCreatedBefore(days));
  }
}
