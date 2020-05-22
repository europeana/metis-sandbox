package eu.europeana.metis.sandbox.scheduler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.service.dataset.DatasetRemoverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetRemoverScheduleTest {

  @Mock
  private DatasetRemoverService datasetRemoverService;

  @InjectMocks
  private DatasetRemoverSchedule datasetRemoverSchedule;

  @BeforeEach
  void init() {
    setField(datasetRemoverSchedule, "daysToPreserve", 7);
  }

  @Test
  void remove_expectSuccess() {
    setField(datasetRemoverSchedule, "cleanupEnable", true);
    datasetRemoverSchedule.remove();
    verify(datasetRemoverService).remove(7);
  }

  @Test
  void remove_scheduleDisable_expectSuccess() {
    setField(datasetRemoverSchedule, "cleanupEnable", false);
    datasetRemoverSchedule.remove();
    verifyNoInteractions(datasetRemoverService);
  }

  @Test
  void remove_failToRemove_expectFail() {
    setField(datasetRemoverSchedule, "cleanupEnable", true);
    doThrow(new ServiceException("", new Exception())).when(datasetRemoverService).remove(7);
    assertThrows(ServiceException.class, () -> datasetRemoverSchedule.remove());
  }
}