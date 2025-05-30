package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import eu.europeana.metis.sandbox.service.problempatterns.ProblemPatternDataRemover;
import eu.europeana.metis.sandbox.service.record.RecordService;
import eu.europeana.metis.sandbox.service.util.ThumbnailStoreService;
import eu.europeana.metis.sandbox.service.util.VacuumService;
import eu.europeana.metis.sandbox.service.workflow.IndexingService;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The type Dataset remover service.
 */
@Service
public class DatasetRemoverService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final DatasetService datasetService;
  private final DatasetLogService datasetLogService;
  private final IndexingService indexingService;
  private final ThumbnailStoreService thumbnailStoreService;
  private final RecordService recordService;
  private final ProblemPatternDataRemover problemPatternDataRemover;
  private final HarvestingParameterService harvestingParameterService;
  private final DeBiasStateService debiasStateService;
  private final VacuumService vacuumService;

  /**
   * Instantiates a new Dataset remover service.
   *
   * @param datasetService the dataset service
   * @param datasetLogService the dataset log service
   * @param recordLogService the record log service
   * @param indexingService the indexing service
   * @param thumbnailStoreService the thumbnail store service
   * @param recordService the record service
   * @param problemPatternDataRemover the problem pattern data remover
   * @param harvestingParameterService the harvesting parameter service
   * @param debiasStateService the debias state service
   * @param vacuumService the vacuum service
   */
  DatasetRemoverService(
          DatasetService datasetService,
          DatasetLogService datasetLogService,
          IndexingService indexingService,
          ThumbnailStoreService thumbnailStoreService,
          RecordService recordService,
          ProblemPatternDataRemover problemPatternDataRemover,
          HarvestingParameterService harvestingParameterService,
          DeBiasStateService debiasStateService,
          VacuumService vacuumService) {
    this.datasetService = datasetService;
    this.datasetLogService = datasetLogService;
    this.indexingService = indexingService;
    this.thumbnailStoreService = thumbnailStoreService;
    this.recordService = recordService;
    this.problemPatternDataRemover = problemPatternDataRemover;
    this.harvestingParameterService = harvestingParameterService;
    this.debiasStateService = debiasStateService;
    this.vacuumService = vacuumService;
  }

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
          LOGGER.info("Remove debias report with id: [{}]", dataset);
          debiasStateService.remove(Integer.valueOf(dataset));
          LOGGER.info("Remove records for dataset id: [{}]", dataset);
          recordService.remove(dataset);
          LOGGER.info("Remove logs for dataset id: [{}]", dataset);
          datasetLogService.remove(dataset);
          LOGGER.info("Remove harvesting parameters for dataset id: [{}]", dataset);
          harvestingParameterService.remove(dataset);
          LOGGER.info("Remove problem pattern data associated with dataset id: [{}]", dataset);
          problemPatternDataRemover.remove(dataset);
          LOGGER.info("Remove dataset with id: [{}]", dataset);
          datasetService.remove(dataset);
        } catch (ServiceException e) {
          LOGGER.error("Failed to remove dataset [{}] ", dataset, e);
        }
      });
      LOGGER.info("Data removal completed, performing vacuum cleaning...");
      vacuumService.vacuum();
    } catch (RuntimeException exception) {
      LOGGER.error("General failure to remove dataset", exception);
    }
  }
}
