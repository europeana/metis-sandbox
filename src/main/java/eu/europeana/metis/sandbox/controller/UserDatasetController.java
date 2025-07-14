package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.security.AuthenticationUtils.getUserId;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
//import com.fasterxml.jackson.annotation.JsonProperty;

//import eu.europeana.metis.sandbox.domain.UserDatasetMetadata;

import eu.europeana.metis.sandbox.dto.UserDatasetDto;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;

import eu.europeana.metis.sandbox.service.dataset.DatasetLogService;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

/*


'creation-date': string;
'created-by-id': string;
'dataset-id': string;
'dataset-name': string;
country: string;
language: string;

  i.e. DatasetInfoBase

  without:
    'harvesting-parameters'
    'transformed-to-edm-external'

  with:

    'harvest-protocol': HarvestProtocol;
    status: DatasetStatus;
    'total-records': number;
    'processed-records': number;

*/
    @Operation(summary = "Get user's datasets", description = "Get user's datasets")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "400", description = "Error")
    @GetMapping(value = "{id}", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UserDatasetDto getUserDatasets(@AuthenticationPrincipal Jwt jwtPrincipal){

      DatasetInfoDto datasetInfo = getDatasetsByCreator(jwtPrincipal);

      ProgressInfoDto progressData = reportService.getReport(datasetInfo.getDatasetId());

      UserDatasetDto userDatasetDto = new UserDatasetDto();

      // commute progressData properties
      userDatasetDto.setStatus(progressData.getStatus());
      userDatasetDto.setTotalRecords(progressData.getTotalRecords());
      userDatasetDto.setProcessedRecords(progressData.getProcessedRecords());

      // commute datasetInfo properties
      userDatasetDto.setDatasetId(datasetInfo.getDatasetId());
      userDatasetDto.setDatasetName(datasetInfo.getDatasetName());

      userDatasetDto.setCountry(datasetInfo.getCountry());
      userDatasetDto.setLanguage(datasetInfo.getLanguage());

      userDatasetDto.setHarvestProtocol(datasetInfo.getHarvestingParametricDto().getHarvestProtocol());
      userDatasetDto.setCreationDate(datasetInfo.getCreationDate());

      return userDatasetDto;
    }


  // TODO: we need a merged list
  private DatasetInfoDto getDatasetsByCreator(
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
        List<String> ids = datasetService.getDatasetIdsCreatedByUser(userId);

        return datasetService.getDatasetInfo(userId);
    }

}
