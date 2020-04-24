package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import java.io.IOException;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class TransformationServiceImplTest {

  private final TestUtils testUtils = new TestUtils();

  @Mock
  private ObjectProvider<XsltTransformer> objectProvider;

  @Mock
  private XsltTransformer xsltTransformer;

  @InjectMocks
  private TransformationServiceImpl service;

  @Test
  void transform_expectSuccess() throws IOException, TransformationException {
    var input = testUtils.readFileToBytes("record/transform/record-input.xml");
    var expected = testUtils.readFileToString("record/transform/record-expected.xml");

    var record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT).content(input)
        .recordId("").build();

    StringWriter writer = new StringWriter();
    writer.write(expected);

    when(objectProvider.getObject(anyString(), anyString(), anyString()))
        .thenReturn(xsltTransformer);
    when(xsltTransformer.transform(any(byte[].class), any(EuropeanaGeneratedIdsMap.class)))
        .thenReturn(writer);

    var result = service.transform(record);

    assertEquals(expected, result.getRecord().getContentString());
  }

  @Test
  void transform_nullRecord_expectFail() {
    assertThrows(NullPointerException.class, () -> service.transform(null));
  }

  @Test
  void transform_invalidXml_expectFail() throws IOException, TransformationException {
    var input = testUtils.readFileToBytes("record/bad-order/record-input.xml");

    var record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT).content(input)
        .recordId("1").build();

    when(objectProvider.getObject(anyString(), anyString(), anyString()))
        .thenReturn(xsltTransformer);
    when(xsltTransformer.transform(any(byte[].class), any(EuropeanaGeneratedIdsMap.class)))
        .thenThrow(new TransformationException(new Exception("Failing here")));

    RecordProcessingException exception = assertThrows(RecordProcessingException.class,
        () -> service.transform(record));
    assertEquals("1", exception.getRecordId());
  }
}