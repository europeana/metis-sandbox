package eu.europeana.metis.sandbox.service.record;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RecordServiceImplTest {

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private XmlRecordProcessorService xmlRecordProcessorService;

    @InjectMocks
    private RecordServiceImpl service;

    @BeforeEach
    void prepare() {
        reset(recordRepository);
    }

    @Test
    void setEuropeanaIdAndProviderId_expectSuccess() {
        byte[] content = "content".getBytes(StandardCharsets.UTF_8);
        Record record = Record.builder()
                .recordId(1L)
                .datasetId("1")
                .datasetName("datasetName")
                .country(Country.NETHERLANDS)
                .language(Language.NL)
                .content(content).build();

        String providerId = "providerId";
        String europeanaId = "/1/providerId";

        when(xmlRecordProcessorService.getProviderId(content)).thenReturn(providerId);
        service.setEuropeanaIdAndProviderId(record);

        verify(recordRepository).updateEuropeanaIdAndProviderId(1L, europeanaId, providerId);
        assertEquals(providerId, record.getProviderId());
        assertEquals(europeanaId, record.getEuropeanaId());
    }
}
