package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.exception.RecordParsingException;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import java.io.ByteArrayInputStream;
import java.util.List;

public interface DatasetGeneratorService {

  /**
   * Creates a {@link Dataset} instance. <br /> Filters out duplicate records
   *
   * @param datasetMetadata must not be null
   * @param records must not be null or empty
   * @return {@link Dataset}
   * @throws NullPointerException if any parameter is null
   * @throws IllegalArgumentException if records list is empty
   * @throws RecordParsingException if fails extracting a record id
   */
  Dataset generate(DatasetMetadata datasetMetadata, List<ByteArrayInputStream> records);
}
