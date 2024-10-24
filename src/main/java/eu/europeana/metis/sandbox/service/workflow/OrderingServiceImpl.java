package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
class OrderingServiceImpl implements OrderingService {

  private final ObjectFactory<XsltTransformer> xsltEdmSorter;

  public OrderingServiceImpl(@Qualifier("xsltEdmSorter") ObjectFactory<XsltTransformer> xsltEdmSorter) {
    this.xsltEdmSorter = xsltEdmSorter;
  }

  @Override
  public byte[] performOrdering(byte[] recordToOrder) throws TransformationException {
    requireNonNull(recordToOrder, "Record must not be null");
    return getEdmSorter().transformToBytes(recordToOrder, null);
  }

  private XsltTransformer getEdmSorter() {
    return xsltEdmSorter.getObject();
  }
}
