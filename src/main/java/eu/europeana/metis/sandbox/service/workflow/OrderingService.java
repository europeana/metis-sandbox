package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.NonRecoverableServiceException;
import eu.europeana.metis.transformation.service.TransformationException;

public interface OrderingService {

  /**
   * Order the provided xml based on edm requirements
   *
   * @param xml must not be null
   * @return String representing the ordered xml
   * @throws NullPointerException           if xml is null
   * @throws TransformationException if ordering fails
   */
  String performOrdering(String xml) throws TransformationException;
}
