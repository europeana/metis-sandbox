package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.mediaprocessing.model.Thumbnail;
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
   */
  void store(List<Thumbnail> thumbnails, String datasetId);

  /**
   * Remove the thumbnails that belong to datasets in the provided list
   *
   * @param datasetIds must not be null
   * @throws NullPointerException if thumbnail list is null
   * @throws ThumbnailRemoveException if there is any issue deleting a thumbnail
   */
  void remove(List<String> datasetIds);
}
