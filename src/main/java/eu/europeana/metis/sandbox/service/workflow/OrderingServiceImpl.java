package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Service;

@Service
class OrderingServiceImpl implements OrderingService {

  private ObjectFactory<XsltTransformer> orderObjectFactory;

  public OrderingServiceImpl(
      ObjectFactory<XsltTransformer> orderObjectFactory) {
    this.orderObjectFactory = orderObjectFactory;
  }

  @Override
  public String performOrdering(String record) throws TransformationException {
    requireNonNull(record, "Record must not be null");
    return getEdmSorter()
        .transform(record.getBytes(StandardCharsets.UTF_8), null)
        .toString();
  }

  private XsltTransformer getEdmSorter() {
    return orderObjectFactory.getObject();
  }
}