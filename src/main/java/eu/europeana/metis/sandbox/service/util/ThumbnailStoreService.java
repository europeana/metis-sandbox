package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.mediaprocessing.model.Thumbnail;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.exception.ThumbnailRemoveException;
import eu.europeana.metis.sandbox.common.exception.ThumbnailStoringException;
import java.util.List;

public interface ThumbnailStoreService {

  /**
   * Store the given thumbnail list, every thumbnail is closed at the end of this process
   *
   * @param thumbnails must not be null
   * @throws NullPointerException if thumbnail list is null
   * @throws ThumbnailStoringException if there is any issue storing a thumbnail
   * @throws ServiceException if any unhandled exception happens, exception will contain original exception
   */
  void store(List<Thumbnail> thumbnails, String datasetId);

  /**
   * Remove the thumbnails that belong to specified dataset
   *
   * @param datasetId must not be null
   * @throws NullPointerException if thumbnail list is null
   * @throws ThumbnailRemoveException if there is any issue deleting a thumbnail
   * @throws ServiceException if any unhandled exception happens, exception will contain original exception
   */
  void remove(String datasetId);
}
