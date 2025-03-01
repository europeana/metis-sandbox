package eu.europeana.metis.sandbox.service.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.indexing.tiers.model.TierResults;
import eu.europeana.indexing.utils.LicenseType;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.RecordDuplicatedException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.dto.RecordTiersInfoDto;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordJdbcRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
import java.nio.charset.StandardCharsets;
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

  @Mock
  private XmlRecordProcessorService xmlRecordProcessorService;

  @Mock
  private RecordJdbcRepository recordJdbcRepository;

  @InjectMocks
  private RecordServiceImpl recordService;

  @BeforeEach
  void prepare() {
    reset(recordRepository);
  }

  @Test
  void getRecordsTiers_expectSuccess(){
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
  void getRecordsTiers_expectException(){
    when(recordRepository.findByDatasetId("datasetId")).thenReturn(Collections.emptyList());
    InvalidDatasetException exception = assertThrows(InvalidDatasetException.class, () ->
            recordService.getRecordsTiers("datasetId"));
    assertEquals("Provided dataset id: [datasetId] is not valid. ", exception.getMessage());

  }

  @Test
  void setEuropeanaIdAndProviderId_expectSuccess() {
    final byte[] content = "content".getBytes(StandardCharsets.UTF_8);
    Record testRecord = getRecord(content);
    final String providerId = "providerId";
    final String europeanaId = "/1/providerId";

    when(xmlRecordProcessorService.getProviderId(content)).thenReturn(providerId);
    when(recordJdbcRepository.updateRecord(anyLong(), anyString(), anyString(), anyString())).thenReturn(1);
    recordService.setEuropeanaIdAndProviderId(testRecord);

    assertEquals(providerId, testRecord.getProviderId());
    assertEquals(europeanaId, testRecord.getEuropeanaId());
  }

  @Test
  void setEuropeanaIdAndProviderId_expectServiceExpection() {
    final byte[] content = "content".getBytes(StandardCharsets.UTF_8);
    Record testRecord = getRecord(content);
    final String datasetId = "1";
    final String providerId = "providerId";
    final String europeanaId = "/1/providerId";

    when(xmlRecordProcessorService.getProviderId(content)).thenReturn(providerId);
    when(recordJdbcRepository.updateRecord(1, europeanaId, providerId, datasetId)).thenReturn(-1);
    ServiceException serviceException = assertThrows(ServiceException.class, () ->
            recordService.setEuropeanaIdAndProviderId(testRecord));

    assertEquals("primary key in record table is corrupted (dataset_id,provider_id,europeana_id)."
            +" providerId & europeanaId updated multiple times",
        serviceException.getMessage());
    assertNull(testRecord.getProviderId());
    assertNull(testRecord.getEuropeanaId());
  }

  @Test
  void setEuropeanaIdAndProviderId_expectProviderAndEuropeanaIdRecordDuplicatedException() {
    final byte[] content = "content".getBytes(StandardCharsets.UTF_8);
    Record testRecord = getRecord(content);
    final String providerId = "providerId";

    when(xmlRecordProcessorService.getProviderId(content)).thenReturn(providerId);
    when(recordJdbcRepository.updateRecord(anyLong(), anyString(), anyString(), anyString())).thenReturn(0);
    RecordDuplicatedException recordDuplicatedException = assertThrows(RecordDuplicatedException.class, () -> {
          recordService.setEuropeanaIdAndProviderId(testRecord);
        }
    );
    assertEquals("Duplicated record has been found: ProviderId: providerId | EuropeanaId: /1/providerId",
            recordDuplicatedException.getMessage());
    assertNull(testRecord.getProviderId());
    assertNull(testRecord.getEuropeanaId());
  }

  @Test
  void remove() {
    recordService.remove("1");
    verify(recordRepository, times(1)).deleteByDatasetId("1");
  }

  @Test
  void testSetTierResults() {
    final byte[] content = "content".getBytes(StandardCharsets.UTF_8);
    TierResults tierResults = mock(TierResults.class);
    when(tierResults.getMediaTier()).thenReturn(MediaTier.T3);
    when(tierResults.getContentTierBeforeLicenseCorrection()).thenReturn(MediaTier.T4);
    when(tierResults.getMetadataTier()).thenReturn(MetadataTier.TA);
    when(tierResults.getMetadataTierLanguage()).thenReturn(MetadataTier.TB);
    when(tierResults.getMetadataTierEnablingElements()).thenReturn(MetadataTier.TC);
    when(tierResults.getMetadataTierContextualClasses()).thenReturn(MetadataTier.T0);
    when(tierResults.getLicenseType()).thenReturn(LicenseType.OPEN);
    recordService.setTierResults(getRecord(content), tierResults);
    verify(recordRepository).updateRecordWithTierResults(anyLong(), eq("3"), eq("A"), eq("4"), eq("B"),
            eq("C"), eq("0"), eq("OPEN"));
  }

  private static Record getRecord(byte[] content) {
    return Record.builder()
                 .recordId(1L)
                 .datasetId("1")
                 .datasetName("datasetName")
                 .country(Country.NETHERLANDS)
                 .language(Language.NL)
                 .content(content).build();
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
