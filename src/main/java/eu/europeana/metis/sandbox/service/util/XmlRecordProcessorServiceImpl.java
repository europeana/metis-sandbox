package eu.europeana.metis.sandbox.service.util;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import eu.europeana.metis.sandbox.common.exception.RecordParsingException;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;

@Service
class XmlRecordProcessorServiceImpl implements XmlRecordProcessorService {

  private static final String RECORD_ID_EXPRESSION = "//*[namespace-uri()=\"http://www.europeana.eu/schemas/edm/\" and local-name()='ProvidedCHO']/@*[namespace-uri()=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" and local-name()='about']";

  private final ObjectFactory<XPathFactory> objectFactory;

  public XmlRecordProcessorServiceImpl(
      ObjectFactory<XPathFactory> objectFactory) {
    this.objectFactory = objectFactory;
  }

  @Override
  public String getRecordId(byte[] record) {
    var xpathFactory = getXPathFactory();
    var xpath = xpathFactory.newXPath();
    var source = new InputSource(new ByteArrayInputStream(record));
    String recordId;

    try {
      String evaluationResult = xpath.evaluate(RECORD_ID_EXPRESSION, source);
      recordId = isEmpty(evaluationResult) ? UUID.randomUUID().toString() : evaluationResult;
    } catch (XPathExpressionException e) {
      throw new RecordParsingException(e);
    }

    if (isEmpty(recordId)) {
      throw new IllegalArgumentException("Provided xml record does not have a valid id");
    }

    return recordId;
  }

  private XPathFactory getXPathFactory() {
    return objectFactory.getObject();
  }
}
