package eu.europeana.metis.sandbox.service.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordLogServiceImplTest {

  @Mock
  RecordErrorLogRepository errorLogRepository;
  @Mock
  RecordRepository recordRepository;
  @Mock
  private RecordLogRepository recordLogRepository;
  @InjectMocks
  private RecordLogServiceImpl service;

  @BeforeEach
  void prepare() {
    reset(errorLogRepository);
    reset(recordLogRepository);
  }

  @Test
  void logRecord_expectSuccess() {
    var testRecord = Record.builder().recordId(1L).content("".getBytes()).datasetId("1")
        .language(Language.IT).country(Country.ITALY).datasetName("").build();

    var event = new RecordProcessEvent(new RecordInfo(testRecord), Step.HARVEST_FILE, Status.SUCCESS);

    service.logRecordEvent(event);

    verify(recordLogRepository).save(any(RecordLogEntity.class));
    verify(errorLogRepository).saveAll(anyList());
  }

  @Test
  void logRecord_nullRecord_expectFail() {
    assertThrows(NullPointerException.class, () -> service.logRecordEvent(null));
  }

  @Test
  void logRecord_unableToSaveRecord_expectFail() {
    var testRecord = Record.builder().recordId(1L).content("".getBytes()).datasetId("1")
        .language(Language.IT).country(Country.ITALY).datasetName("").build();

    var event = new RecordProcessEvent(new RecordInfo(testRecord), Step.HARVEST_FILE, Status.SUCCESS);

    when(recordLogRepository.save(any(RecordLogEntity.class)))
        .thenThrow(new RuntimeException("Exception saving"));

    assertThrows(ServiceException.class, () -> service.logRecordEvent(event));
  }

  @Test
  void logRecord_unableToSaveRecordErrors_expectFail() {
    var testRecord = Record.builder().recordId(1L).content("".getBytes()).datasetId("1")
        .language(Language.IT).country(Country.ITALY).datasetName("").build();

    var event = new RecordProcessEvent(new RecordInfo(testRecord), Step.HARVEST_FILE, Status.SUCCESS);

    when(errorLogRepository.saveAll(anyList()))
        .thenThrow(new RuntimeException("Exception saving"));

    assertThrows(ServiceException.class, () -> service.logRecordEvent(event));
  }

  @Test
  void remove_expectSuccess() {
    service.remove("1");
    verify(errorLogRepository).deleteByRecordIdDatasetId("1");
    verify(recordLogRepository).deleteByRecordIdDatasetId("1");
  }

  @Test
  void remove_errorOnDelete_expectFail() {
    doThrow(new ServiceException("Failed", new Exception())).when(recordLogRepository)
        .deleteByRecordIdDatasetId("1");
    assertThrows(ServiceException.class, () -> service.remove("1"));
  }

  @Test
  void remove_nullInput_expectFail() {
    assertThrows(NullPointerException.class, () -> service.remove(null));
  }

  @Test
  void getProviderRecordStringByStep_expectSuccess() throws Exception {
    final RecordLogEntity recordLogEntity = new RecordLogEntity();
    recordLogEntity.setContent("content");
    when(recordLogRepository.findRecordLogByRecordIdDatasetIdAndStepIn("recordId", "datasetId",
        Set.of(Step.HARVEST_OAI_PMH, Step.HARVEST_FILE))).thenReturn(Set.of(recordLogEntity));
    final String providerRecord = service.getProviderRecordString("recordId", "datasetId",
        Set.of(Step.HARVEST_OAI_PMH, Step.HARVEST_FILE));
    assertNotNull(providerRecord);
    assertEquals("content", providerRecord);
  }

  @Test
  void getProviderRecordString_expectFail() {
    //Case null entity
    when(recordLogRepository.findRecordLogByRecordIdDatasetIdAndStepIn("recordId", "datasetId",
        Set.of(Step.HARVEST_OAI_PMH, Step.HARVEST_FILE))).thenReturn(Collections.emptySet());
    assertThrows(NoRecordFoundException.class,
        () -> service.getProviderRecordString("recordId", "datasetId", Set.of(Step.HARVEST_OAI_PMH, Step.HARVEST_FILE)));

    //Case null content
    when(recordLogRepository.findRecordLogByRecordIdDatasetIdAndStepIn("recordId", "datasetId",
        Set.of(Step.HARVEST_OAI_PMH, Step.HARVEST_FILE))).thenReturn(Set.of(new RecordLogEntity()));
    assertThrows(NoRecordFoundException.class,
        () -> service.getProviderRecordString("recordId", "datasetId", Set.of(Step.HARVEST_OAI_PMH, Step.HARVEST_FILE)));
  }

  @Test
  void getRecordLogEntity() {
    service.getRecordLogEntity("recordId", "datasetId", Step.MEDIA_PROCESS);
    verify(recordLogRepository).findRecordLogByRecordIdDatasetIdAndStep("recordId", "datasetId",
        Step.MEDIA_PROCESS);
    clearInvocations(recordLogRepository);

    //Case PROVIDER_ID
    service.getRecordLogEntity("recordId", "datasetId", Step.MEDIA_PROCESS);
    verify(recordLogRepository).findRecordLogByRecordIdDatasetIdAndStep("recordId", "datasetId",
        Step.MEDIA_PROCESS);
  }

}
