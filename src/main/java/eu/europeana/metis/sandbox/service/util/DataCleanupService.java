package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.service.dataset.DatasetDataCleaner;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import eu.europeana.metis.sandbox.service.problempatterns.ProblemPatternDataCleaner;
import eu.europeana.metis.sandbox.service.record.ExecutionRecordCleaner;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * The type Dataset remover service.
 */
@Slf4j
@AllArgsConstructor
@Service
public class DataCleanupService {

  private final DatasetDataCleaner datasetDataCleaner;
  private final DatasetReportService datasetReportService;
  private final ExecutionRecordCleaner executionRecordCleaner;
  private final IndexDataCleaner indexDataCleaner;
  private final ThumbnailStoreService thumbnailStoreService;
  private final ProblemPatternDataCleaner problemPatternDataCleaner;
  private final HarvestParameterService harvestParameterService;
  private final DeBiasStateService debiasStateService;

  /**
   * Removes datasets and associated entities created before the specified number of days.
   *
   * @param days the number of days before which datasets and their related data are removed
   */
  public void remove(int days) {
    try {
      List<String> datasets = datasetReportService.findDatasetIdsByCreatedBefore(days);

      log.info("Datasets to remove {} ", datasets);

      datasets.forEach(dataset -> {
        try {
          // remove thumbnails (s3)
          log.info("Remove thumbnails for dataset id: [{}]", dataset);
          thumbnailStoreService.remove(dataset);
          // remove from mongo and solr
          log.info("Remove index for dataset id: [{}]", dataset);
          indexDataCleaner.remove(dataset);
          log.info("Remove debias report with id: [{}]", dataset);
          debiasStateService.remove(dataset);
          log.info("Remove harvesting parameters for dataset id: [{}]", dataset);
          harvestParameterService.remove(dataset);
          log.info("Remove problem pattern data associated with dataset id: [{}]", dataset);
          problemPatternDataCleaner.remove(dataset);
          log.info("Remove execution records for dataset id: [{}]", dataset);
          executionRecordCleaner.remove(dataset);
          log.info("Remove dataset with id: [{}]", dataset);
          datasetDataCleaner.remove(dataset);
        } catch (ServiceException e) {
          log.error("Failed to remove dataset [{}] ", dataset, e);
        }
      });
    } catch (RuntimeException exception) {
      log.error("General failure to remove dataset", exception);
    }
  }
}
