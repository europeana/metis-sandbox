package eu.europeana.metis.sandbox.service.workflow;

public interface OrderingService {

  /**
   * Order the provided xml based on edm requirements
   *
   * @param xml must not be null
   * @return String representing the ordered xml
   * @throws NullPointerException if xml is null
   */
  String performOrdering(String xml);
}
