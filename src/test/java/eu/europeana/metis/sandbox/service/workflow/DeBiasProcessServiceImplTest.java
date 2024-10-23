package eu.europeana.metis.sandbox.service.workflow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.debias.detect.client.DeBiasClient;
import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.repository.debias.DatasetDeBiasRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasDetailRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasMainRepository;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeBiasProcessServiceImplTest {

  @Mock
  DeBiasClient deBiasClient;
  @Mock
  RecordDeBiasMainRepository recordDeBiasMainRepository;
  @Mock
  RecordDeBiasDetailRepository recordDeBiasDetailRepository;
  @Mock
  DatasetDeBiasRepository datasetDeBiasRepository;
  @Mock
  RecordLogRepository recordLogRepository;
  @Mock
  RecordRepository recordRepository;

  @InjectMocks
  DeBiasProcessServiceImpl deBiasProcessService;

  @Test
  void processSuccess() throws IOException {
    String testRecord1 = new TestUtils().readFileToString("record"+ File.separator+"debias"+File.separator+"debias-text-record.xml");
    String testRecord2 = new TestUtils().readFileToString("record"+ File.separator+"debias"+File.separator+"debias-video-record.xml");
    var testRecords = List.of(
        Record.builder()
              .recordId(1L)
              .europeanaId("europeanaId1")
              .content(testRecord1.getBytes(StandardCharsets.UTF_8))
              .language(Language.NL).country(Country.NETHERLANDS)
              .datasetName("datasetName")
              .datasetId("1")
              .build(),
        Record.builder()
              .recordId(2L)
              .europeanaId("europeanaId2")
              .content(testRecord2.getBytes())
              .language(Language.NL).country(Country.NETHERLANDS)
              .datasetName("datasetName")
              .datasetId("1")
              .build());

    deBiasProcessService.process(testRecords);

    verify(deBiasClient,times(5)).detect(any());
  }
}
