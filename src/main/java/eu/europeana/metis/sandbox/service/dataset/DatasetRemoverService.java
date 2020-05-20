package eu.europeana.metis.sandbox.service.dataset;

public interface DatasetRemoverService {

  /**
   * Remove datasets older than the given amount of days.
   * <br />
   * The date of the purge will be days counting from the date of the upload of the data.
   * @param days before to preserve data
   */
  void remove(int days);
}
