package eu.europeana.metis.sandbox.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.exception.RecordParsingException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectFactory;
import org.xml.sax.InputSource;

import java.io.File;

@ExtendWith(MockitoExtension.class)
class XmlRecordProcessorServiceImplTest {

  private final TestUtils utils = new TestUtils();

  @Mock
  private ObjectFactory<XPathFactory> objectFactory;

  @Mock
  private XPath xPath;

  @Spy
  private final XPathFactory xPathFactory = XPathFactory.newDefaultInstance();

  @InjectMocks
  private XmlRecordProcessorServiceImpl service;

  @Test
  void getRecordId_expectSuccess() throws Exception {

    var xmlRecord = utils.readFileToBytes("record"+File.separator+"valid-record.xml");

    when(objectFactory.getObject()).thenReturn(xPathFactory);

    String recordId = service.getProviderId(xmlRecord);

    assertEquals("URN:NBN:SI:doc-35SZSOCF", recordId);
  }

  @Test
  void getRecordId_nullRecord_expectFail() {
    assertThrows(NullPointerException.class, () -> service.getProviderId(null));
  }

  @Test
  void getRecordId_recordParsingException_expectFail() throws Exception {
    var xmlRecord = utils.readFileToBytes("record"+File.separator+"valid-record.xml");

    when(objectFactory.getObject()).thenReturn(xPathFactory);
    when(xPathFactory.newXPath()).thenReturn(xPath);
    when(xPath.evaluate(any(String.class), any(InputSource.class)))
        .thenThrow(new XPathExpressionException("Fail here"));

    assertThrows(RecordParsingException.class, () -> service.getProviderId(xmlRecord));
  }

  @Test
  void getRecordId_recordMissingId_expectFail() throws Exception {
    var xmlRecord = utils.readFileToBytes("record"+File.separator+"record-missing-id.xml");

    when(objectFactory.getObject()).thenReturn(xPathFactory);

    assertThrows(IllegalArgumentException.class, () -> service.getProviderId(xmlRecord));
  }
}