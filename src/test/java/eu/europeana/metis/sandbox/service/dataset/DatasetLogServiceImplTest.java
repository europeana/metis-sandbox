package eu.europeana.metis.sandbox.service.dataset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.DatasetLogEntity;
import eu.europeana.metis.sandbox.repository.DatasetLogRepository;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;



@ExtendWith(MockitoExtension.class)
class DatasetLogServiceImplTest {

  private static final String DATASET_ID = "1";
  public static final String MESSAGE_1 = "ExceptionMessage1";

  public static final String MESSAGE_2 = "ExceptionMessage2";

  public static final String MESSAGE_3 = "ExceptionMessage3";
  @Mock
  private DatasetRepository datasetRepository;
  @Mock
  private DatasetLogRepository datasetLogRepository;
  @InjectMocks
  private DatasetLogServiceImpl service;
  @Mock
  private DatasetEntity dataset;
  @Captor
  private ArgumentCaptor<DatasetLogEntity> logCaptor;

  @BeforeEach
  public void setup() {
    when(datasetRepository.findById(1)).thenReturn(Optional.of(dataset));
  }

  @Test
  public void logException_shouldLogMessageFromSingleException() {
    Throwable e = new RuntimeException(MESSAGE_1);
    
    service.logException(DATASET_ID, e);

    verify(datasetLogRepository).save(logCaptor.capture());
    DatasetLogEntity savedLog = logCaptor.getValue();
    assertThat(savedLog.getMessage()).contains(MESSAGE_1);
    assertThat(savedLog.getStackTrace()).isNotBlank();
    assertThat(savedLog.getStatus()).isEqualTo(Status.FAIL);
    assertThat(savedLog.getDataset()).isEqualTo(dataset);
  }

  @Test
  public void logException_shouldLogExceptionWithNullMessage() {
    Throwable e = new RuntimeException();

    service.logException(DATASET_ID, e);

    verify(datasetLogRepository).save(logCaptor.capture());
    DatasetLogEntity savedLog = logCaptor.getValue();
    assertThat(savedLog.getMessage()).contains(RuntimeException.class.getName());
    assertThat(savedLog.getStackTrace()).isNotBlank();
    assertThat(savedLog.getStatus()).isEqualTo(Status.FAIL);
    assertThat(savedLog.getDataset()).isEqualTo(dataset);
  }

  @Test
  public void logException_shouldLogMessageFromExceptionChain() {
    Throwable e = new RuntimeException(MESSAGE_3,new Exception(MESSAGE_2, new RuntimeException(MESSAGE_1)));

    service.logException(DATASET_ID, e);

    verify(datasetLogRepository).save(logCaptor.capture());
    DatasetLogEntity savedLog = logCaptor.getValue();
    assertThat(savedLog.getMessage()).contains(MESSAGE_1);
    assertThat(savedLog.getMessage()).contains(MESSAGE_2);
    assertThat(savedLog.getMessage()).contains(MESSAGE_3);
    assertThat(savedLog.getStackTrace()).isNotBlank();
    assertThat(savedLog.getStatus()).isEqualTo(Status.FAIL);
    assertThat(savedLog.getDataset()).isEqualTo(dataset);
  }

  @Test
  public void logException_shouldUnwrapComletionException() {
    Throwable e = new CompletionException(MESSAGE_2,new RuntimeException(MESSAGE_1));

    service.logException(DATASET_ID, e);

    verify(datasetLogRepository).save(logCaptor.capture());
    DatasetLogEntity savedLog = logCaptor.getValue();
    assertThat(savedLog.getMessage()).contains(MESSAGE_1);
    assertThat(savedLog.getMessage()).doesNotContain(MESSAGE_2);
    assertThat(savedLog.getStackTrace()).isNotBlank();
    assertThat(savedLog.getStatus()).isEqualTo(Status.FAIL);
    assertThat(savedLog.getDataset()).isEqualTo(dataset);
  }


}