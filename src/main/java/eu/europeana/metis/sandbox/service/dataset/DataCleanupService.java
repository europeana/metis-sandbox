package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import eu.europeana.metis.sandbox.service.problempatterns.ProblemPatternDataCleaner;
import eu.europeana.metis.sandbox.service.record.ExecutionRecordCleaner;
import eu.europeana.metis.sandbox.service.util.IndexDataCleaner;
import eu.europeana.metis.sandbox.service.util.ThumbnailStoreService;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The type Dataset remover service.
 */
@Service
public class DataCleanupService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final DatasetService datasetService;
  private final ExecutionRecordCleaner executionRecordCleaner;
  private final IndexDataCleaner indexDataCleaner;
  private final ThumbnailStoreService thumbnailStoreService;
  private final ProblemPatternDataCleaner problemPatternDataCleaner;
  private final HarvestingParameterService harvestingParameterService;
  private final DeBiasStateService debiasStateService;

  /**
   * Instantiates a new Dataset remover service.
   *
   * @param datasetService the dataset service
   * @param datasetLogService the dataset log service
   * @param recordLogService the record log service
   * @param indexDataCleaner the indexing service
   * @param thumbnailStoreService the thumbnail store service
   * @param recordService the record service
   * @param problemPatternDataCleaner the problem pattern data remover
   * @param harvestingParameterService the harvesting parameter service
   * @param debiasStateService the debias state service
   * @param vacuumService the vacuum service
   */
  DataCleanupService(
      DatasetService datasetService, ExecutionRecordCleaner executionRecordCleaner,
      IndexDataCleaner indexDataCleaner,
      ThumbnailStoreService thumbnailStoreService,
      ProblemPatternDataCleaner problemPatternDataCleaner,
      HarvestingParameterService harvestingParameterService,
      DeBiasStateService debiasStateService) {
    this.datasetService = datasetService;
    this.executionRecordCleaner = executionRecordCleaner;
    this.indexDataCleaner = indexDataCleaner;
    this.thumbnailStoreService = thumbnailStoreService;
    this.problemPatternDataCleaner = problemPatternDataCleaner;
    this.harvestingParameterService = harvestingParameterService;
    this.debiasStateService = debiasStateService;
  }

  public void remove(int days) {
    try {
      // get old dataset ids
      List<String> datasets = datasetService.findDatasetIdsByCreatedBefore(days);

      LOGGER.info("Datasets to remove {} ", datasets);

      datasets.forEach(dataset -> {
        try {
          // remove thumbnails (s3)
          LOGGER.info("Remove thumbnails for dataset id: [{}]", dataset);
          thumbnailStoreService.remove(dataset);
          // remove from mongo and solr
          LOGGER.info("Remove index for dataset id: [{}]", dataset);
          indexDataCleaner.remove(dataset);
          LOGGER.info("Remove debias report with id: [{}]", dataset);
          debiasStateService.remove(dataset);
          LOGGER.info("Remove harvesting parameters for dataset id: [{}]", dataset);
          harvestingParameterService.remove(dataset);
          LOGGER.info("Remove problem pattern data associated with dataset id: [{}]", dataset);
          problemPatternDataCleaner.remove(dataset);
          LOGGER.info("Remove execution records for dataset id: [{}]", dataset);
          executionRecordCleaner.remove(dataset);
          LOGGER.info("Remove dataset with id: [{}]", dataset);
          datasetService.remove(dataset);
        } catch (ServiceException e) {
          LOGGER.error("Failed to remove dataset [{}] ", dataset, e);
        }
      });
    } catch (RuntimeException exception) {
      LOGGER.error("General failure to remove dataset", exception);
    }
  }
}
