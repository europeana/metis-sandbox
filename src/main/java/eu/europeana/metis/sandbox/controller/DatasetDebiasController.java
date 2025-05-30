package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.sandbox.controller.DatasetController.MESSAGE_FOR_400_CODE;
import static eu.europeana.metis.security.AuthenticationUtils.getUserId;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.debias.DeBiasReportDto;
import eu.europeana.metis.sandbox.dto.debias.DeBiasStatusDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto.Status;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import eu.europeana.metis.sandbox.service.engine.BatchJobExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller that handles API endpoints related to dataset debiasing operations. This controller provides endpoints to process
 * debiasing on datasets, retrieve debiasing reports, and check debiasing status.
 */
@RestController
@RequestMapping("/dataset/")
@Tag(name = "Dataset Debias Controller")
public class DatasetDebiasController {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final DatasetService datasetService;
  private final DeBiasStateService debiasStateService;
  private final DatasetReportService datasetReportService;
  private final Map<Integer, Lock> datasetIdLocksMap = new ConcurrentHashMap<>();
  private final LockRegistry lockRegistry;
  private final BatchJobExecutor batchJobExecutor;

  /**
   * Constructs a new instance of {@link DatasetDebiasController}.
   *
   * @param datasetService the service to handle dataset operations
   * @param debiasStateService the service to manage DeBias state
   * @param datasetReportService the service to handle dataset reports
   * @param lockRegistry the registry for managing dataset locks
   */
  @Autowired
  public DatasetDebiasController(DatasetService datasetService, DeBiasStateService debiasStateService,
      DatasetReportService datasetReportService, LockRegistry lockRegistry, BatchJobExecutor batchJobExecutor) {
    this.datasetService = datasetService;
    this.debiasStateService = debiasStateService;
    this.datasetReportService = datasetReportService;
    this.lockRegistry = lockRegistry;
    this.batchJobExecutor = batchJobExecutor;
  }

  /**
   * Process DeBias boolean.
   *
   * @param jwtPrincipal the authenticated user's JWT token
   * @param datasetId the dataset id
   * @return the boolean
   */
  @Operation(description = "Process debias detection dataset")
  @ApiResponse(responseCode = "200", description = "Process debias detection feature", content = {
      @Content(mediaType = APPLICATION_JSON_VALUE)})
  @ApiResponse(responseCode = "400", description = MESSAGE_FOR_400_CODE)
  @PostMapping(value = "{id}/debias", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public boolean processDeBias(@AuthenticationPrincipal Jwt jwtPrincipal, @PathVariable("id") Integer datasetId) {
    final DatasetInfoDto datasetInfo = datasetService.getDatasetInfo(datasetId.toString());

    //Check ownership
    if (StringUtils.isNotBlank(datasetInfo.getCreatedById())) {
      if (jwtPrincipal == null) {
        return false;
      }

      final String userId = getUserId(jwtPrincipal);
      if (!datasetInfo.getCreatedById().equals(userId)) {
        LOGGER.warn("User {} is not the owner of dataset {}. Ignoring request.", userId, datasetId);
        return false;
      }
    }

    final Lock lock = datasetIdLocksMap.computeIfAbsent(datasetId, s -> lockRegistry.obtain("debiasProcess_" + datasetId));
    try {
      lock.lock();
      LOGGER.info("DeBias process: {} lock, Locked", datasetId);
      ProgressInfoDto progressInfoDto = datasetReportService.getProgress(datasetId.toString());

      if (progressInfoDto.getStatus().equals(Status.COMPLETED) &&
          "READY".equals(Optional.ofNullable(debiasStateService.getDeBiasStatus(String.valueOf(datasetId)))
                                 .map(DeBiasStatusDto::getState)
                                 .orElse(""))) {
        debiasStateService.remove(datasetId);

        DatasetDeBiasEntity datasetDeBiasEntity = debiasStateService.createDatasetDeBiasEntity(datasetId);
        DatasetEntity datasetEntity = datasetDeBiasEntity.getDatasetId();
        DatasetMetadata datasetMetadata = DatasetMetadata.builder().withDatasetId(datasetId.toString())
                                                         .withDatasetName(datasetEntity.getDatasetName())
                                                         .withCountry(datasetEntity.getCountry())
                                                         .withLanguage(datasetEntity.getLanguage())
                                                         .build();
        batchJobExecutor.execute(datasetMetadata);
        return true;
      } else {
        return false;
      }
    } finally {
      lock.unlock();
      LOGGER.info("DeBias process: {} lock, Unlocked", datasetId);
    }
  }

  /**
   * Gets DeBias detection information.
   *
   * @param datasetId the dataset id
   * @return the DeBias detection
   */
  @Operation(description = "Get Bias detection report for a dataset")
  @ApiResponse(responseCode = "200", description = "Get detection information about DeBias detection", content = {
      @Content(mediaType = APPLICATION_JSON_VALUE)})
  @ApiResponse(responseCode = "400", description = MESSAGE_FOR_400_CODE)
  @GetMapping(value = "{id}/debias/report", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public DeBiasReportDto getDeBiasReport(@PathVariable("id") Integer datasetId) {
    return debiasStateService.getDeBiasReport(String.valueOf(datasetId));
  }

  /**
   * Gets DeBias detection information.
   *
   * @param datasetId the dataset id
   * @return the DeBias detection
   */
  @Operation(description = "Get DeBias detection status for a dataset")
  @ApiResponse(responseCode = "200", description = "Get status about DeBias detection", content = {
      @Content(mediaType = APPLICATION_JSON_VALUE)})
  @ApiResponse(responseCode = "400", description = MESSAGE_FOR_400_CODE)
  @GetMapping(value = "{id}/debias/info", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public DeBiasStatusDto getDeBiasStatus(@PathVariable("id") Integer datasetId) {
    return debiasStateService.getDeBiasStatus(String.valueOf(datasetId));
  }
}
