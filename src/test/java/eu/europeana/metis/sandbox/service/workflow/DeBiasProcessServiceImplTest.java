package eu.europeana.metis.sandbox.service.workflow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.debias.detect.client.DeBiasClient;
import eu.europeana.metis.debias.detect.model.request.BiasInputLiterals;
import eu.europeana.metis.debias.detect.model.response.DetectionDeBiasResult;
import eu.europeana.metis.debias.detect.model.response.Tag;
import eu.europeana.metis.debias.detect.model.response.ValueDetection;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
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
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.support.locks.LockRegistry;

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
  @Mock
  LockRegistry lockRegistry;

  @InjectMocks
  DeBiasProcessServiceImpl deBiasProcessService;

  private DetectionDeBiasResult getDetectionDeBiasResult(Language language) {
    DetectionDeBiasResult result = new DetectionDeBiasResult();
    ValueDetection vdEmpty = new ValueDetection();
    vdEmpty.setLiteral("");
    vdEmpty.setLanguage(language.name().toLowerCase(Locale.US));
    vdEmpty.setTags(List.of());
    switch (language) {
      case NL:
        ValueDetection vdNl1 = new ValueDetection();
        vdNl1.setLiteral("Meester en slaaf Provinciale Openbare Bibliotheek in Krakau voor de mensheid");
        vdNl1.setLanguage(language.name().toLowerCase(Locale.US));
        Tag tagNl1 = new Tag();
        tagNl1.setStart(11);
        tagNl1.setEnd(16);
        tagNl1.setLength(5);
        tagNl1.setUri("http://www.example.org/debias#t_135_nl");
        vdNl1.setTags(List.of(tagNl1));

        ValueDetection vdNl2 = new ValueDetection();
        vdNl2.setLiteral("Panoramafilm van linkpad 1-5, Skara Brae, verslaafde en aboriginal");
        vdNl2.setLanguage(language.name().toLowerCase(Locale.US));
        Tag tagNl2 = new Tag();
        tagNl2.setStart(56);
        tagNl2.setEnd(66);
        tagNl2.setLength(10);
        tagNl2.setUri("http://www.example.org/debias#t_1_nl");
        vdNl2.setTags(List.of(tagNl2));

        ValueDetection vdNl3 = new ValueDetection();
        vdNl3.setLiteral("Settlement clans");
        vdNl3.setLanguage(language.name().toLowerCase(Locale.US));
        Tag tagNl3 = new Tag();
        tagNl3.setStart(11);
        tagNl3.setEnd(16);
        tagNl3.setLength(5);
        tagNl3.setUri("http://www.example.org/debias#t_24_nl");
        vdNl3.setTags(List.of(tagNl3));

        result.setDetections(List.of(vdNl1, vdNl2, vdNl3, vdEmpty, vdEmpty));
        break;
      case DE:
        ValueDetection vdDe1 = new ValueDetection();
        vdDe1.setLiteral("Herr und Sklave Öffentliche Provinzbibliothek in Krakau für die Menschheit");
        vdDe1.setLanguage(language.name().toLowerCase(Locale.US));
        Tag tagDe1 = new Tag();
        tagDe1.setStart(9);
        tagDe1.setEnd(15);
        tagDe1.setLength(6);
        tagDe1.setUri("http://www.example.org/debias#t_77_de");
        vdDe1.setTags(List.of(tagDe1));

        ValueDetection vdDe2 = new ValueDetection();
        vdDe2.setLiteral("Panoramafilm des Verbindungspfads 1-5, Skara Brae, Süchtiger und Ureinwohner");
        vdDe2.setLanguage(language.name().toLowerCase(Locale.US));
        Tag tagDe2 = new Tag();
        tagDe2.setStart(65);
        tagDe2.setEnd(76);
        tagDe2.setLength(11);
        tagDe2.setUri("http://www.example.org/debias#t_87_de");
        vdDe2.setTags(List.of(tagDe2));

        result.setDetections(List.of(vdDe1, vdDe2, vdEmpty, vdEmpty, vdEmpty));
        break;
      case EN:
        ValueDetection vdEn1 = new ValueDetection();
        vdEn1.setLiteral("Master and slave Provincial Public Library in Krakow");
        vdEn1.setLanguage(language.name().toLowerCase(Locale.US));
        Tag tagEn1 = new Tag();
        tagEn1.setStart(11);
        tagEn1.setEnd(16);
        tagEn1.setLength(5);
        tagEn1.setUri("http://www.example.org/debias#t_198_en");
        vdEn1.setTags(List.of(tagEn1));

        ValueDetection vdEn2 = new ValueDetection();
        vdEn2.setLiteral("Panorama Movie of link path 1-5, Skara Brae addict and aboriginal");
        vdEn2.setLanguage(language.name().toLowerCase(Locale.US));
        Tag tagEn21 = new Tag();
        tagEn21.setStart(44);
        tagEn21.setEnd(50);
        tagEn21.setLength(7);
        tagEn21.setUri("http://www.example.org/debias#t_3_en");
        Tag tagEn22 = new Tag();
        tagEn22.setStart(55);
        tagEn22.setEnd(65);
        tagEn22.setLength(10);
        tagEn22.setUri("http://www.example.org/debias#t_2_en");
        vdEn2.setTags(List.of(tagEn21, tagEn22));

        ValueDetection vdEn3 = new ValueDetection();
        vdEn3.setLiteral("Settlement clans");
        vdEn3.setLanguage(language.name().toLowerCase(Locale.US));
        Tag tagEn3 = new Tag();
        tagEn3.setStart(11);
        tagEn3.setEnd(16);
        tagEn3.setLength(5);
        tagEn3.setUri("http://www.example.org/debias#t_46_en");
        vdEn3.setTags(List.of(tagEn3));

        ValueDetection vdEn4 = new ValueDetection();
        vdEn4.setLiteral("Movie housewife, guy, chairman, addict, victims, homemaker");
        vdEn4.setLanguage(language.name().toLowerCase(Locale.US));
        Tag tagEn4 = new Tag();
        tagEn4.setStart(32);
        tagEn4.setEnd(38);
        tagEn4.setLength(6);
        tagEn4.setUri("http://www.example.org/debias#t_3_en");
        vdEn4.setTags(List.of(tagEn4));

        result.setDetections(List.of(vdEn1, vdEn2, vdEn3, vdEn4, vdEmpty));
        break;
      case IT, FR:
        result.setDetections(List.of(vdEmpty, vdEmpty,
            vdEmpty, vdEmpty, vdEmpty));
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + language);
    }
    return result;
  }

  @Test
  void processSuccess() throws IOException {
    String testRecord1 = new TestUtils().readFileToString(
        "record" + File.separator + "debias" + File.separator + "debias-text-record.xml");
    String testRecord2 = new TestUtils().readFileToString(
        "record" + File.separator + "debias" + File.separator + "debias-video-record.xml");
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
    when(lockRegistry.obtain(anyString())).thenReturn(new ReentrantLock());
    when(deBiasClient.detect(any(BiasInputLiterals.class)))
        .thenReturn(getDetectionDeBiasResult(Language.NL))
        .thenReturn(getDetectionDeBiasResult(Language.FR))
        .thenReturn(getDetectionDeBiasResult(Language.IT))
        .thenReturn(getDetectionDeBiasResult(Language.DE))
        .thenReturn(getDetectionDeBiasResult(Language.EN));
    deBiasProcessService.process(testRecords);

    verify(deBiasClient, times(5)).detect(any());
    verify(datasetDeBiasRepository, times(1)).updateState(anyInt(), eq("COMPLETED"));
    verify(recordDeBiasMainRepository, times(9)).save(any());
    verify(recordDeBiasDetailRepository, times(10)).save(any());
    verify(recordLogRepository, times(2)).updateByRecordIdAndStepAndStatus(anyLong(), eq(Step.DEBIAS), eq(Status.SUCCESS));
    verify(recordLogRepository, times(1)).getTotalDeBiasCounterByDatasetId(anyString());
    verify(recordLogRepository, times(1)).getProgressDeBiasCounterByDatasetId(anyString());
    verify(recordRepository, times(9)).findById(anyLong());
    verify(lockRegistry, times(1)).obtain(anyString());
  }
}
