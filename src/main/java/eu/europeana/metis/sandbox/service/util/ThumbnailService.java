package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.mediaprocessing.model.Thumbnail;
import java.util.List;

public interface ThumbnailService {

  void storeThumbnails(List<Thumbnail> results);
}
