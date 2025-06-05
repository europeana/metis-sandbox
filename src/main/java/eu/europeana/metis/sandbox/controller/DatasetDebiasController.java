package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.security.AuthenticationUtils.getUserId;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.ExceptionModelDTO;
import eu.europeana.metis.sandbox.dto.debias.DeBiasReportDTO;
import eu.europeana.metis.sandbox.dto.debias.DeBiasStatusDTO;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.invoke.MethodHandles;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

  /**
   * Constructs a new instance of {@link DatasetDebiasController}.
   *
   * @param datasetService the service to handle dataset operations
   * @param debiasStateService the service to manage DeBias state
   * @param datasetReportService the service to handle dataset reports
   * @param lockRegistry the registry for managing dataset locks
   */
  @Autowired
  public DatasetDebiasController(DatasetService datasetService, DeBiasStateService debiasStateService) {
    this.datasetService = datasetService;
    this.debiasStateService = debiasStateService;
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
  @ApiResponse(responseCode = "400")
  @PostMapping(value = "{id}/debias", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public boolean processDeBias(@AuthenticationPrincipal Jwt jwtPrincipal, @PathVariable("id") Integer datasetId) {
    final DatasetInfoDTO datasetInfo = datasetService.getDatasetInfo(datasetId.toString());

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
    return datasetService.createAndExecuteDatasetForDebias(String.valueOf(datasetId));
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
  @ApiResponse(responseCode = "400")
  @GetMapping(value = "{id}/debias/report", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public DeBiasReportDTO getDeBiasReport(@PathVariable("id") Integer datasetId) {
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
  @ApiResponse(responseCode = "400")
  @GetMapping(value = "{id}/debias/info", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public DeBiasStatusDTO getDeBiasStatus(@PathVariable("id") Integer datasetId) {
    return debiasStateService.getDeBiasStatus(String.valueOf(datasetId));
  }
}
