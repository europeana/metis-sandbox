package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.NonRecoverableServiceException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Service;

@Service
@Slf4j
class DefaultOrderingService implements OrderingService {

  private ObjectFactory<XsltTransformer> orderObjectFactory;

  public DefaultOrderingService(
      ObjectFactory<XsltTransformer> orderObjectFactory) {
    this.orderObjectFactory = orderObjectFactory;
  }

  /*var xslt = new StreamSource(edmSorterUrl);
    var text = new StreamSource(new ByteArrayInputStream(record.getContent().getBytes(StandardCharsets.UTF_8)));
    var writer = new StringWriter();
    Transformer transformer;
    String recordOrdered;
    try {
      transformer = TransformerFactory.newInstance().newTransformer(xslt);
      transformer.transform(text, new StreamResult(writer));
    } catch (TransformerException e) {
      throw new NonRecoverableServiceException("Ordering of the record failed " + e.getMessage(), e);
    }

    log.info(writer.toString());
    recordOrdered = writer.toString();*/

  @Override
  public String performOrdering(String record) {
    requireNonNull(record, "Record must not be null");
    StringWriter writer;
    try {
      writer = getEdmSorter().transform(record.getBytes(StandardCharsets.UTF_8), null);
    } catch (TransformationException e) {
      throw new NonRecoverableServiceException("", e);
    }

    var recordOrdered = writer.toString();

    log.info("This is the input: {}", record.getContent());

    log.info("This is the output: {}", recordOrdered);

    return recordOrdered;
  }

  private XsltTransformer getEdmSorter() {
    return orderObjectFactory.getObject();
  }
}
