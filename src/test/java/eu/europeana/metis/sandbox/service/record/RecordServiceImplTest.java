package eu.europeana.metis.sandbox.service.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;

import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.dto.RecordTiersInfoDto;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordServiceImplTest {

  @Mock
  private RecordRepository recordRepository;

  @InjectMocks
  private RecordServiceImpl recordService;

  @BeforeEach
  void prepare() {
    reset(recordRepository);
  }

  @Test
  void getRecordsTiers_Old_expectSuccess(){
    RecordEntity recordEntity1 = new RecordEntity.RecordEntityBuilder()
            .setEuropeanaId("europeanaId1")
            .setProviderId("providerId1")
            .setDatasetId("datasetId")
            .setContentTier("3")
            .setContentTierBeforeLicenseCorrection("4")
            .setMetadataTier("A")
            .setMetadataTierLanguage("B")
            .setMetadataTierEnablingElements("C")
            .setMetadataTierContextualClasses("0")
            .setLicense("OPEN")
            .build();
    RecordEntity recordEntity2 = new RecordEntity.RecordEntityBuilder()
            .setEuropeanaId("europeanaId2")
            .setProviderId("providerId2")
            .setDatasetId("datasetId")
            .setContentTier("2")
            .setContentTierBeforeLicenseCorrection("2")
            .setMetadataTier("B")
            .setMetadataTierLanguage("C")
            .setMetadataTierEnablingElements("0")
            .setMetadataTierContextualClasses("A")
            .setLicense("RESTRICTED")
            .build();
    RecordEntity recordEntity3 = new RecordEntity.RecordEntityBuilder()
            .setEuropeanaId("europeanaId3")
            .setProviderId("providerId3")
            .setDatasetId("datasetId")
            .setContentTier("1")
            .setContentTierBeforeLicenseCorrection("1")
            .setMetadataTier("C")
            .setMetadataTierLanguage("0")
            .setMetadataTierEnablingElements("B")
            .setMetadataTierContextualClasses("A")
            .setLicense("CLOSED")
            .build();
    List<RecordEntity> recordEntities = List.of(recordEntity1, recordEntity2, recordEntity3);
    when(recordRepository.findByDatasetId("datasetId")).thenReturn(recordEntities);
    List<RecordTiersInfoDto> result = recordService.getRecordsTiers("datasetId");
    checkTierValues(recordEntity1, result.get(0));
    checkTierValues(recordEntity2, result.get(1));
    checkTierValues(recordEntity3, result.get(2));
  }

  @Test
  void getRecordsTiers_Old_expectException(){
    when(recordRepository.findByDatasetId("datasetId")).thenReturn(Collections.emptyList());
    InvalidDatasetException exception = assertThrows(InvalidDatasetException.class, () ->
            recordService.getRecordsTiers("datasetId"));
    assertEquals("Provided dataset id: [datasetId] is not valid. ", exception.getMessage());

  }

  @Test
  void remove() {
    recordService.remove("1");
    verify(recordRepository, times(1)).deleteByDatasetId("1");
  }

  private void checkTierValues(RecordEntity recordEntity, RecordTiersInfoDto recordTiersInfoDto){
    assertEquals(recordEntity.getContentTier(), recordTiersInfoDto.getContentTier().toString());
    assertEquals(recordEntity.getContentTierBeforeLicenseCorrection(), recordTiersInfoDto.getContentTierBeforeLicenseCorrection().toString());
    assertEquals(recordEntity.getMetadataTier(), recordTiersInfoDto.getMetadataTier().toString());
    assertEquals(recordEntity.getMetadataTierLanguage(), recordTiersInfoDto.getMetadataTierLanguage().toString());
    assertEquals(recordEntity.getMetadataTierEnablingElements(), recordTiersInfoDto.getMetadataTierEnablingElements().toString());
    assertEquals(recordEntity.getMetadataTierContextualClasses(), recordTiersInfoDto.getMetadataTierContextualClasses().toString());
    assertEquals(recordEntity.getLicense(), recordTiersInfoDto.getLicense().toString());

  }
}
