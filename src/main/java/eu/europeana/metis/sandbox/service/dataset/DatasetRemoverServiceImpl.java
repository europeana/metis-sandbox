package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.util.ThumbnailService;
import eu.europeana.metis.sandbox.service.workflow.IndexingService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class DatasetRemoverServiceImpl implements DatasetRemoverService {

  private final DatasetService datasetService;
  private final RecordLogService recordLogService;
  private final IndexingService indexingService;
  private final ThumbnailService thumbnailService;

  DatasetRemoverServiceImpl(
      DatasetService datasetService,
      RecordLogService recordLogService,
      IndexingService indexingService,
      ThumbnailService thumbnailService) {
    this.datasetService = datasetService;
    this.recordLogService = recordLogService;
    this.indexingService = indexingService;
    this.thumbnailService = thumbnailService;
  }

  @Override
  public void remove(int days) {
    // get old dataset ids
    List<String> datasets = datasetService.getDatasetIdsBefore(days);

    // remove thumbnails (s3)
    thumbnailService.remove(datasets);

    // remove from mongo and solr
    indexingService.remove(datasets);

    // remove from sandbox
    recordLogService.removeByDatasetIds(datasets);
    datasetService.removeByDatasetIds(datasets);
  }
}
