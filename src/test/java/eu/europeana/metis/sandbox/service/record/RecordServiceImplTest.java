package eu.europeana.metis.sandbox.service.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.indexing.tiers.model.TierResults;
import eu.europeana.metis.sandbox.common.exception.RecordDuplicatedException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.repository.RecordJdbcRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
import java.nio.charset.StandardCharsets;
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
  void setEuropeanaIdAndProviderId_expectSuccess() {
    final byte[] content = "content".getBytes(StandardCharsets.UTF_8);
    Record record = getRecord(content);
    final String providerId = "providerId";
    final String europeanaId = "/1/providerId";

    when(xmlRecordProcessorService.getProviderId(content)).thenReturn(providerId);
    when(recordJdbcRepository.updateRecord(anyLong(), anyString(), anyString(), anyString())).thenReturn(1);
    recordService.setEuropeanaIdAndProviderId(record);

    assertEquals(providerId, record.getProviderId());
    assertEquals(europeanaId, record.getEuropeanaId());
  }

  @Test
  void setEuropeanaIdAndProviderId_expectServiceExpection() {
    final byte[] content = "content".getBytes(StandardCharsets.UTF_8);
    Record record = getRecord(content);
    final String datasetId = "1";
    final String providerId = "providerId";
    final String europeanaId = "/1/providerId";

    when(xmlRecordProcessorService.getProviderId(content)).thenReturn(providerId);
    when(recordJdbcRepository.updateRecord(1, europeanaId, providerId, datasetId)).thenReturn(-1);
    ServiceException serviceException = assertThrows(ServiceException.class, () ->
            recordService.setEuropeanaIdAndProviderId(record));

    assertEquals("primary key in record table is corrupted (dataset_id,provider_id,europeana_id)."
            +" providerId & europeanaId updated multiple times",
        serviceException.getMessage());
    assertNull(record.getProviderId());
    assertNull(record.getEuropeanaId());
  }

  @Test
  void setEuropeanaIdAndProviderId_expectProviderAndEuropeanaIdRecordDuplicatedException() {
    final byte[] content = "content".getBytes(StandardCharsets.UTF_8);
    Record record = getRecord(content);
    final String providerId = "providerId";

    when(xmlRecordProcessorService.getProviderId(content)).thenReturn(providerId);
    when(recordJdbcRepository.updateRecord(anyLong(), anyString(), anyString(), anyString())).thenReturn(0);
    RecordDuplicatedException recordDuplicatedException = assertThrows(RecordDuplicatedException.class, () -> {
          recordService.setEuropeanaIdAndProviderId(record);
        }
    );
    assertEquals("Duplicated record has been found: ProviderId: providerId | EuropeanaId: /1/providerId",
            recordDuplicatedException.getMessage());
    assertNull(record.getProviderId());
    assertNull(record.getEuropeanaId());
  }

  @Test
  void remove() {
    recordService.remove("1");
    verify(recordRepository, times(1)).deleteByDatasetId("1");
  }

  @Test
  void setContentTierAndMetadataTier() {
    final byte[] content = "content".getBytes(StandardCharsets.UTF_8);
    TierResults tierResults = mock(TierResults.class);
    when(tierResults.getMediaTier()).thenReturn(MediaTier.T4);
    when(tierResults.getMetadataTier()).thenReturn(MetadataTier.TA);
    recordService.setTierResults(getRecord(content), tierResults);
    verify(recordRepository).updateRecordWithTierResults(anyLong(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString());
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
}
