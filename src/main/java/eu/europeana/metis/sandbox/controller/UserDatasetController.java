package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.security.AuthenticationUtils.getUserId;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import eu.europeana.metis.sandbox.dto.UserDatasetDto;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;

import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type User Dataset controller.
 */
@RestController
@Tag(name = "User Dataset Controller")
class UserDatasetController {

  private final DatasetService datasetService;
  private final DatasetReportService reportService;

    @Autowired
    public UserDatasetController(
      DatasetService datasetService,
      DatasetReportService reportService
     ) {
        this.datasetService = datasetService;
        this.reportService = reportService;
    }

    /**
     * getUserDatasets
     *
     * loads the users datasets and combines them with their progress data
     */
    @Operation(summary = "Get user's datasets", description = "Get user's datasets")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "404", description = "User Datatsets not found")
    @ApiResponse(responseCode = "400", description = "Error")
    @GetMapping(value = "/user-datasets", produces = APPLICATION_JSON_VALUE)
    public List<UserDatasetDto> getUserDatasets(@AuthenticationPrincipal Jwt jwtPrincipal){

      List<DatasetInfoDto> datasetInfos = getDatasetsByCreator(jwtPrincipal);
      List<UserDatasetDto> userDatasetDtos = new ArrayList<>();

      for (int i = 0; i < datasetInfos.size(); i++){
        DatasetInfoDto datasetInfoDto = datasetInfos.get(i);
        ProgressInfoDto progressData = reportService.getReport(datasetInfoDto.getDatasetId());

        UserDatasetDto userDatasetDto = new UserDatasetDto();
        userDatasetDtos.add(userDatasetDto);

        // commute progressData properties
        userDatasetDto.setStatus(progressData.getStatus());
        userDatasetDto.setTotalRecords(progressData.getTotalRecords());
        userDatasetDto.setProcessedRecords(progressData.getProcessedRecords());

        // commute datasetInfo properties
        userDatasetDto.setDatasetId(datasetInfoDto.getDatasetId());
        userDatasetDto.setDatasetName(datasetInfoDto.getDatasetName());

        userDatasetDto.setCountry(datasetInfoDto.getCountry());
        userDatasetDto.setLanguage(datasetInfoDto.getLanguage());

        userDatasetDto.setHarvestProtocol(datasetInfoDto.getHarvestingParametricDto().getHarvestProtocol());
        userDatasetDto.setCreationDate(datasetInfoDto.getCreationDate());
      }

      return userDatasetDtos;
    }

  /**
   * getDatasetsByCreator
   *
   * gets the user's datasets
   */
  private List<DatasetInfoDto> getDatasetsByCreator(
      @AuthenticationPrincipal Jwt jwtPrincipal
    ) {
        final String userId;
        if (jwtPrincipal == null) {
          userId = null;
        } else {
          userId = getUserId(jwtPrincipal);
        }
        return datasetService.getDatasetsCreatedById(userId);
    }

}
