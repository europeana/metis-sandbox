package eu.europeana.metis.sandbox.service.workflow;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface HarvestServiceOaiPmh {

  List<ByteArrayInputStream> harvestOaiPmh(String endpoint, String setSpec, String prefix);

}
