package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.exception.ServiceException;

public interface DatasetRemoverService {

  /**
   * Remove datasets older than the given amount of days.
   * <br />
   * The date of the purge will be days counting from the date of the upload of the data.
   *
   * @param days before to preserve data
   * @throws ServiceException if there is an issue looking for datasets to delete
   *
   * @implNote if there are errors deleting a specific dataset, it will be logged as an error
   * and process will continue, there will be no exceptions thrown
   */
  void remove(int days);
}
