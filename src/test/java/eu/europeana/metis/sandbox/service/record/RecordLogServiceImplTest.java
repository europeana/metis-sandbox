package eu.europeana.metis.sandbox.service.record;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordLogServiceImplTest {

  @Mock
  private RecordLogRepository repository;

  @InjectMocks
  private RecordLogServiceImpl service;

  @Test
  void logRecord_expectSuccess() {
    Record record = Record.builder().recordId("").content("").datasetId("")
        .language(Language.IT).country(Country.ITALY).datasetName("").build();

    Event event = new Event(record, Step.CREATE);

    service.logRecordEvent(event);

    verify(repository).save(any(RecordLogEntity.class));
  }

  @Test
  void logRecord_nullRecord_expectFail() {
    assertThrows(NullPointerException.class, () -> service.logRecordEvent(null));
  }

  @Test
  void logRecord_unableToSave_expectFail() {
    Record record = Record.builder().recordId("").content("").datasetId("")
        .language(Language.IT).country(Country.ITALY).datasetName("").build();

    Event event = new Event(record, Step.CREATE);

    when(repository.save(any(RecordLogEntity.class)))
        .thenThrow(new RuntimeException("Exception saving"));

    assertThrows(ServiceException.class, () -> service.logRecordEvent(event));
  }
}