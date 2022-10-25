package eu.europeana.metis.sandbox.service.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.metis.sandbox.common.exception.RecordDuplicatedException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
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
    final String datasetId = "1";
    final String providerId = "providerId";
    final String europeanaId = "/1/providerId";

    when(xmlRecordProcessorService.getProviderId(content)).thenReturn(providerId);
    when(recordRepository.updateEuropeanaIdAndProviderId(anyLong(), anyString(), anyString(), anyString())).thenReturn(1);
    recordService.setEuropeanaIdAndProviderId(record);

    verify(recordRepository).updateEuropeanaIdAndProviderId(1L, europeanaId, providerId, datasetId);
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
    when(recordRepository.updateEuropeanaIdAndProviderId(anyLong(), anyString(), anyString(), anyString())).thenReturn(2);
    ServiceException serviceException = assertThrows(ServiceException.class, () -> recordService.setEuropeanaIdAndProviderId(record));

    assertEquals("primary key in record table is corrupted (dataset_id,provider_id,europeana_id)."
            +" providerId & europeanaId updated multiple times",
        serviceException.getMessage());
    verify(recordRepository).updateEuropeanaIdAndProviderId(1L, europeanaId, providerId, datasetId);
    assertEquals(null, record.getProviderId());
    assertEquals(null, record.getEuropeanaId());
  }

  @Test
  void setEuropeanaIdAndProviderId_expectProviderAndEuropeanaIdRecordDuplicatedException() {
    final byte[] content = "content".getBytes(StandardCharsets.UTF_8);
    Record record = getRecord(content);
    final String datasetId = "1";
    final String providerId = "providerId";
    final String europeanaId = "/1/providerId";

    when(xmlRecordProcessorService.getProviderId(content)).thenReturn(providerId);
    when(recordRepository.updateEuropeanaIdAndProviderId(anyLong(), anyString(), anyString(), anyString())).thenReturn(0);
    RecordDuplicatedException recordDuplicatedException = assertThrows(RecordDuplicatedException.class, () -> {
          recordService.setEuropeanaIdAndProviderId(record);
        }
    );
    assertEquals("ProviderId: providerId | EuropeanaId: /1/providerId is duplicated.", recordDuplicatedException.getMessage());
    verify(recordRepository, times(1)).updateEuropeanaIdAndProviderId(1L, europeanaId, providerId, datasetId);
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
    recordService.setContentTierAndMetadataTier(getRecord(content), MediaTier.T4, MetadataTier.TA);
    verify(recordRepository).updateContentTierAndMetadataTier(anyLong(), anyString(), anyString());
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
