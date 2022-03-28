package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.exception.RecordParsingException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import java.io.InputStream;
import java.util.List;

public interface DatasetService {

  /**
   * Creates a dataset id and publishes the given records for further processing
   *
   * @param datasetName must not be null
   * @param country     must not be null
   * @param language    must not be null
   * @return the created dataset
   * @throws NullPointerException   if any input is null
   * @throws ServiceException       if any unhandled exception happens, exception will contain
   *                                original exception
   * @throws RecordParsingException if fails to parse a record from the records list
   * @see Dataset
   */
  String createEmptyDataset(String datasetName, Country country, Language language,
      InputStream xsltEdmExternalContentStream);

  /**
   * Get dataset ids created before than the specified days
   *
   * @param days to ignore
   * @return dataset ids
   * @throws ServiceException if any unhandled exception happens, exception will contain original
   *                          exception
   */
  List<String> getDatasetIdsCreatedBefore(int days);

  /**
   * Remove matching dataset id
   *
   * @param datasetId must not be null
   * @throws NullPointerException if dataset id is null
   * @throws ServiceException     if removing dataset fails
   */
  void remove(String datasetId);

  /**
   * Updates the value of recordQuantity in the database to the given dataset
   * @param datasetId The id of the dataset to update to
   * @param numberOfRecords The new value to update into the dataset
   */
  void updateNumberOfTotalRecord(String datasetId, Long numberOfRecords);

  /**
   * Sets to true the boolean recordLimitExceeded in the database
   * @param datasetId The id of the dataset to update this into
   */
  void setRecordLimitExceeded(String datasetId);

  /**
   * A boolean type of query to check if dataset has xslt content in the database
   * @param datasetId The id of the dataset to update into
   * @return Returns 0 if there is no xslt, 1 otherwise
   */
  boolean isXsltPresent(String datasetId);
}
