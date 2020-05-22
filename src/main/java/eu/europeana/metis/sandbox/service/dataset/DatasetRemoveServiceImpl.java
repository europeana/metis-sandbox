package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.util.ThumbnailStoreService;
import eu.europeana.metis.sandbox.service.workflow.IndexingService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class DatasetRemoveServiceImpl implements DatasetRemoverService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatasetRemoveServiceImpl.class);

  private final DatasetService datasetService;
  private final RecordLogService recordLogService;
  private final IndexingService indexingService;
  private final ThumbnailStoreService thumbnailStoreService;

  DatasetRemoveServiceImpl(
      DatasetService datasetService,
      RecordLogService recordLogService,
      IndexingService indexingService,
      ThumbnailStoreService thumbnailStoreService) {
    this.datasetService = datasetService;
    this.recordLogService = recordLogService;
    this.indexingService = indexingService;
    this.thumbnailStoreService = thumbnailStoreService;
  }

  @Override
  public void remove(int days) {
    // get old dataset ids
    List<String> datasets = datasetService.getDatasetIdsBefore(days);

    datasets.forEach(dataset -> {
      try {
        // remove thumbnails (s3)
        thumbnailStoreService.remove(dataset);
        // remove from mongo and solr
        indexingService.remove(dataset);
        // remove from sandbox
        recordLogService.remove(dataset);
        datasetService.remove(dataset);
      } catch (ServiceException e) {
        LOGGER.error("Failed to remove dataset {} ", dataset, e);
      }
    });
  }
}
