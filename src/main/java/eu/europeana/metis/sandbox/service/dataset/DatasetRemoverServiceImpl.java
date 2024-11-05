package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import eu.europeana.metis.sandbox.service.problempatterns.ProblemPatternDataRemover;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.record.RecordService;
import eu.europeana.metis.sandbox.service.util.ThumbnailStoreService;
import eu.europeana.metis.sandbox.service.workflow.IndexingService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class DatasetRemoverServiceImpl implements DatasetRemoverService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatasetRemoverServiceImpl.class);

  private final DatasetService datasetService;
  private final DatasetLogService datasetLogService;
  private final RecordLogService recordLogService;
  private final IndexingService indexingService;
  private final ThumbnailStoreService thumbnailStoreService;
  private final RecordService recordService;
  private final ProblemPatternDataRemover problemPatternDataRemover;
  private final HarvestingParameterService harvestingParameterService;
  private final DeBiasStateService debiasStateService;
  DatasetRemoverServiceImpl(
          DatasetService datasetService,
          DatasetLogService datasetLogService,
          RecordLogService recordLogService,
          IndexingService indexingService,
          ThumbnailStoreService thumbnailStoreService,
          RecordService recordService,
          ProblemPatternDataRemover problemPatternDataRemover,
          HarvestingParameterService harvestingParameterService,
          DeBiasStateService debiasStateService) {
    this.datasetService = datasetService;
    this.datasetLogService = datasetLogService;
    this.recordLogService = recordLogService;
    this.indexingService = indexingService;
    this.thumbnailStoreService = thumbnailStoreService;
    this.recordService = recordService;
    this.problemPatternDataRemover = problemPatternDataRemover;
    this.harvestingParameterService = harvestingParameterService;
    this.debiasStateService = debiasStateService;
  }

  @Override
  public void remove(int days) {
    try {
      // get old dataset ids
      List<String> datasets = datasetService.getDatasetIdsCreatedBefore(days);

      LOGGER.info("Datasets to remove {} ", datasets);

      datasets.forEach(dataset -> {
        try {
          // remove thumbnails (s3)
          LOGGER.info("Remove thumbnails for dataset id: [{}]", dataset);
          thumbnailStoreService.remove(dataset);
          // remove from mongo and solr
          LOGGER.info("Remove index for dataset id: [{}]", dataset);
          indexingService.remove(dataset);
          // remove from sandbox
          LOGGER.info("Remove record logs for dataset id: [{}]", dataset);
          recordLogService.remove(dataset);
          LOGGER.info("Remove records for dataset id: [{}]", dataset);
          recordService.remove(dataset);
          LOGGER.info("Remove logs for dataset id: [{}]", dataset);
          datasetLogService.remove(dataset);
          LOGGER.info("Remove harvesting parameters for dataset id: [{}]", dataset);
          harvestingParameterService.remove(dataset);
          LOGGER.info("Remove problem pattern data associated with dataset id: [{}]", dataset);
          problemPatternDataRemover.removeProblemPatternDataFromDatasetId(dataset);
          LOGGER.info("Remove dataset with id: [{}]", dataset);
          datasetService.remove(dataset);
          LOGGER.info("Remove debias report with id: [{}]", dataset);
          debiasStateService.cleanDeBiasReport(Integer.valueOf(dataset));
        } catch (ServiceException e) {
          LOGGER.error("Failed to remove dataset [{}] ", dataset, e);
        }
      });
    } catch (RuntimeException exception) {
      LOGGER.error("General failure to remove dataset", exception);
    }
  }
}
