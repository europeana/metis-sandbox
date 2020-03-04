package eu.europeana.metis.sandbox.service.record;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultRecordLogServiceTest {

  @Mock
  private RecordLogRepository repository;

  @InjectMocks
  private DefaultRecordLogService service;

  @Test
  void logRecord_expectSuccess() {
    Record record = Record.builder().recordId("").content("").datasetId("").status(Status.SUCCESS)
        .step(Step.CREATE).language(Language.IT).country(Country.ITALY).datasetName("").build();

    service.logRecord(record);

    verify(repository, times(1)).save(any(RecordLogEntity.class));
  }

  @Test
  void logRecord_nullRecord_expectFail() {
    assertThrows(NullPointerException.class, () -> service.logRecord(null));
  }

  @Test
  void logRecord_unableToSave_expectFail() {
    Record record = Record.builder().recordId("").content("").datasetId("").status(Status.SUCCESS)
        .step(Step.CREATE).language(Language.IT).country(Country.ITALY).datasetName("").build();

    when(repository.save(any(RecordLogEntity.class)))
        .thenThrow(new RuntimeException("Exception saving"));

    assertThrows(ServiceException.class, () -> service.logRecord(record));
  }
}