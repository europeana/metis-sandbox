package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.metis.sandbox.common.exception.DatasetRemoveException;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class DatasetRemoverServiceImpl implements DatasetRemoverService {

  private Indexer indexer;
  private final DatasetService datasetService;
  private final RecordLogService recordLogService;

  DatasetRemoverServiceImpl(
      DatasetService datasetService,
      RecordLogService recordLogService) {
    this.datasetService = datasetService;
    this.recordLogService = recordLogService;
  }

  @Override
  public void remove(int days) {
    // get old dataset ids
    List<String> datasets = datasetService.getDatasetIdsBefore(days);

    // remove thumbnails (s3)

    // remove from mongo and solr
    datasets.forEach(dataset -> {
      try {
        indexer.removeAll(dataset, null);
      } catch (IndexingException e) {
        throw new DatasetRemoveException(dataset, e);
      }
    });

    // remove from sandbox
    recordLogService.removeByDatasetIds(datasets);
    datasetService.removeByDatasetIds(datasets);
  }
}
