package eu.europeana.metis.sandbox.controller;



import static eu.europeana.metis.security.AuthenticationUtils.getUserId;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

//import com.fasterxml.jackson.annotation.JsonProperty;
//import eu.europeana.metis.sandbox.domain.UserDatasetMetadata;

import eu.europeana.metis.sandbox.dto.UserDatasetDto;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;

//
import eu.europeana.metis.sandbox.entity.DatasetEntity;


import eu.europeana.metis.sandbox.service.dataset.DatasetLogService;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


/**
 * The type User Dataset controller.
 */
@RestController
@RequestMapping("/user-datasets")
@Tag(name = "User Dataset Controller")
class UserDatasetController {

  private final DatasetService datasetService;
  private final DatasetLogService datasetLogService;
  private final DatasetReportService reportService;

    @Autowired
    public UserDatasetController(
      DatasetService datasetService,
      DatasetLogService datasetLogService,
      DatasetReportService reportService
     ) {
        this.datasetService = datasetService;
        this.datasetLogService = datasetLogService;
        this.reportService = reportService;
    }

    @Operation(summary = "Get user's datasets", description = "Get user's datasets")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "400", description = "Error")
    @GetMapping(value = "{id}", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<UserDatasetDto> getUserDatasets(@AuthenticationPrincipal Jwt jwtPrincipal){

      List<DatasetInfoDto> datasetInfos = getDatasetsByCreator(jwtPrincipal);
      List<UserDatasetDto> userDatasetDtos = new ArrayList<UserDatasetDto>();// getDatasetsByCreator(jwtPrincipal);

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


  // TODO: we need a merged list
//  private List<DatasetEntity> getDatasetsByCreator(
  private List<DatasetInfoDto> getDatasetsByCreator(
      @AuthenticationPrincipal Jwt jwtPrincipal
    )
    {
        final String userId;
        if (jwtPrincipal == null) {
          userId = null;
        } else {
          userId = getUserId(jwtPrincipal);
        }
        // TODO: I want to change this to return an array of my new type
        //return ...
        //

      //  List<DatasetEntity> ids = datasetService.getDatasetIdsCreatedById(userId);

        List<DatasetInfoDto> result = new ArrayList<DatasetInfoDto>();
        result.add(
          datasetService.getDatasetInfo(userId)
        );
        return result;//  datasetService.getDatasetInfo(userId);
    }

}
