package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetDataCleaner;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import eu.europeana.metis.sandbox.service.problempatterns.ProblemPatternDataCleaner;
import eu.europeana.metis.sandbox.service.record.ExecutionRecordCleaner;
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

  private final DatasetDataCleaner datasetDataCleaner;
  private final DatasetReportService datasetReportService;
  private final ExecutionRecordCleaner executionRecordCleaner;
  private final IndexDataCleaner indexDataCleaner;
  private final ThumbnailStoreService thumbnailStoreService;
  private final ProblemPatternDataCleaner problemPatternDataCleaner;
  private final HarvestParameterService harvestParameterService;
  private final DeBiasStateService debiasStateService;

  /**
   * Constructor.
   *
   * @param datasetDataCleaner datasetService is responsible for managing dataset operations.
   * @param datasetReportService datasetReportService provides functionality for dataset reports.
   * @param executionRecordCleaner executionRecordCleaner handles removal of execution records.
   * @param indexDataCleaner indexDataCleaner manages cleanup of indexed data.
   * @param thumbnailStoreService thumbnailStoreService removes thumbnails linked to datasets.
   * @param problemPatternDataCleaner problemPatternDataCleaner handles cleanup of problem pattern data.
   * @param harvestParameterService harvestParameterService manages removal of harvest parameters.
   * @param debiasStateService debiasStateService is responsible for managing debias state cleanup.
   */
  DataCleanupService(
      DatasetDataCleaner datasetDataCleaner, DatasetReportService datasetReportService,
      ExecutionRecordCleaner executionRecordCleaner,
      IndexDataCleaner indexDataCleaner,
      ThumbnailStoreService thumbnailStoreService,
      ProblemPatternDataCleaner problemPatternDataCleaner,
      HarvestParameterService harvestParameterService,
      DeBiasStateService debiasStateService) {
    this.datasetDataCleaner = datasetDataCleaner;
    this.datasetReportService = datasetReportService;
    this.executionRecordCleaner = executionRecordCleaner;
    this.indexDataCleaner = indexDataCleaner;
    this.thumbnailStoreService = thumbnailStoreService;
    this.problemPatternDataCleaner = problemPatternDataCleaner;
    this.harvestParameterService = harvestParameterService;
    this.debiasStateService = debiasStateService;
  }

  /**
   * Removes datasets and associated entities created before the specified number of days.
   *
   * @param days the number of days before which datasets and their related data are removed
   */
  public void remove(int days) {
    try {
      List<String> datasets = datasetReportService.findDatasetIdsByCreatedBefore(days);

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
          harvestParameterService.remove(dataset);
          LOGGER.info("Remove problem pattern data associated with dataset id: [{}]", dataset);
          problemPatternDataCleaner.remove(dataset);
          LOGGER.info("Remove execution records for dataset id: [{}]", dataset);
          executionRecordCleaner.remove(dataset);
          LOGGER.info("Remove dataset with id: [{}]", dataset);
          datasetDataCleaner.remove(dataset);
        } catch (ServiceException e) {
          LOGGER.error("Failed to remove dataset [{}] ", dataset, e);
        }
      });
    } catch (RuntimeException exception) {
      LOGGER.error("General failure to remove dataset", exception);
    }
  }
}
