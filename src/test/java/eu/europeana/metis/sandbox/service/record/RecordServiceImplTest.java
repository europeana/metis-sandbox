package eu.europeana.metis.sandbox.service.record;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService.RecordIdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecordServiceImplTest {

  @Mock
  private RecordRepository recordRepository;

  @InjectMocks
  private RecordServiceImpl service;

  @BeforeEach
  void prepare() {
    reset(recordRepository);
  }

  @Test
  void getProviderRecordString_expectSuccess() throws Exception {
    final RecordEntity recordEntity = new RecordEntity();
    recordEntity.setContent("content");
    when(recordRepository.findRecordEntityByEuropeanaIdAndDatasetId("recordId", "datasetId"))
        .thenReturn(recordEntity);
    assertNotNull(service.getProviderRecordString(RecordIdType.EUROPEANA_ID, "recordId", "datasetId"));
  }

  @Test
  void getProviderRecordString_expectFail() {
    //Case null entity
    when(recordRepository.findRecordEntityByEuropeanaIdAndDatasetId("recordId", "datasetId")).thenReturn(null);
    assertThrows(NoRecordFoundException.class, ()-> service.getProviderRecordString(RecordIdType.EUROPEANA_ID, "recordId", "datasetId"));

    //Case null content
    when(recordRepository.findRecordEntityByEuropeanaIdAndDatasetId("recordId", "datasetId")).thenReturn(new RecordEntity());
    assertThrows(NoRecordFoundException.class, ()-> service.getProviderRecordString(RecordIdType.EUROPEANA_ID, "recordId", "datasetId"));
  }

  @Test
  void getRecordLogEntity() {
    //Case EUROPEANA_ID
    service.getRecordEntity(RecordIdType.EUROPEANA_ID, "recordId", "datasetId");
    verify(recordRepository).findRecordEntityByEuropeanaIdAndDatasetId("recordId", "datasetId");
    verify(recordRepository, never()).findRecordEntityByIdAndDatasetId(anyLong(), anyString());
    clearInvocations(recordRepository);

    //Case PROVIDER_ID
    service.getRecordEntity(RecordIdType.PROVIDER_ID, "recordId", "datasetId");
    verify(recordRepository).findRecordEntityByIdAndDatasetId(1L, "datasetId");
    verify(recordRepository, never()).findRecordEntityByEuropeanaIdAndDatasetId(anyString(), anyString());
  }

}
