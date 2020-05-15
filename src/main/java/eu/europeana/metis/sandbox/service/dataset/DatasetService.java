package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.exception.RecordParsingException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import java.io.ByteArrayInputStream;
import java.util.List;

public interface DatasetService {

  /**
   * Creates a dataset id and publishes the given records for further processing
   *
   * @param datasetName must not be null
   * @param country     must not be null
   * @param language    must not be null
   * @param records     must not be null
   * @return the dataset id
   * @throws NullPointerException     if any input is null
   * @throws IllegalArgumentException if records list is empty
   * @throws ServiceException         if any unhandled exception happens, exception will contain
   *                                  original exception
   * @throws RecordParsingException   if fails to parse a record from the records list
   */
  String createDataset(String datasetName, Country country, Language language,
      List<ByteArrayInputStream> records);
}
