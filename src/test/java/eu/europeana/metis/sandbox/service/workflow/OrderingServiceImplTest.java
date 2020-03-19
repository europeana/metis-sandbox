package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import java.io.IOException;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectFactory;

@ExtendWith(MockitoExtension.class)
class OrderingServiceImplTest {

  private TestUtils testUtils = new TestUtils();

  @Mock
  private ObjectFactory<XsltTransformer> orderObjectFactory;

  @Mock
  private XsltTransformer xsltSorter;

  @InjectMocks
  private OrderingServiceImpl service;

  @Test
  void performOrdering_expectSuccess() throws IOException, TransformationException {
    var input = testUtils.readFileToString("record/bad-order/record-input.xml");
    var expected = testUtils.readFileToString("record/bad-order/record-expected.xml");

    StringWriter writer = new StringWriter();
    writer.write(expected);

    when(orderObjectFactory.getObject()).thenReturn(xsltSorter);
    when(xsltSorter.transform(any(byte[].class), nullable(EuropeanaGeneratedIdsMap.class)))
        .thenReturn(writer);

    String result = service.performOrdering(input);

    assertEquals(expected, result);
  }

  @Test
  void performOrdering_nullXml_expectFail() {
    assertThrows(NullPointerException.class, () -> service.performOrdering(null));
  }

  @Test
  void performOrdering_invalidXml_expectFail() throws IOException, TransformationException {
    var input = testUtils.readFileToString("record/bad-order/record-input.xml");

    when(orderObjectFactory.getObject()).thenReturn(xsltSorter);
    when(xsltSorter.transform(any(byte[].class), nullable(EuropeanaGeneratedIdsMap.class)))
        .thenThrow(new TransformationException(new Exception("Failing here")));

    assertThrows(TransformationException.class, () -> service.performOrdering(input));
  }
}