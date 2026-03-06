package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.Integer.valueOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetDataCleanerTest {

  @Mock
  private DatasetRepository datasetRepository;

  @InjectMocks
  private DatasetDataCleaner datasetDataCleaner;

  @Test
  void remove_shouldDeleteDataset() {
    String datasetId = "123";
    datasetDataCleaner.remove(datasetId);
    verify(datasetRepository).deleteById(valueOf(datasetId));
  }

  @Test
  void remove_shouldThrowServiceExceptionWhenRepositoryThrows() {
    String datasetId = "123";
    doThrow(new RuntimeException()).when(datasetRepository).deleteById(valueOf(datasetId));
    assertThrows(ServiceException.class, () -> datasetDataCleaner.remove(datasetId));
  }
}
