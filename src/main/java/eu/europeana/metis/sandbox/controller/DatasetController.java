package eu.europeana.metis.sandbox.controller;

import static com.google.common.base.Preconditions.checkArgument;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.DatasetIdDto;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ReportByStepDto;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.util.ZipService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/")
@Api(value = "Dataset Controller")
class DatasetController {

  private static final Pattern namePattern = Pattern.compile("[a-zA-Z0-9_-]+");

  @Value("${sandbox.dataset.max-size}")
  private int maxRecords;

  private final ZipService zipService;
  private final DatasetService datasetService;

  public DatasetController(ZipService zipService, DatasetService datasetService) {
    this.zipService = zipService;
    this.datasetService = datasetService;
  }

  @ApiOperation("Process the given dataset")
  @PostMapping(value = "dataset/{name}/process", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public DatasetIdDto processDataset(
      @ApiParam(value = "name of the dataset", required = true) @PathVariable(value = "name") String datasetName,
      @ApiParam(value = "country of the dataset", required = true, defaultValue = "Netherlands") @RequestParam Country country,
      @ApiParam(value = "language of the dataset", required = true, defaultValue = "nl") @RequestParam Language language,
      @ApiParam(value = "dataset records in a zip file", required = true) @RequestParam MultipartFile dataset) {
    checkArgument(namePattern.matcher(datasetName).matches(),
        "dataset name can only include letters, numbers, _ or - characters");
    var records = zipService.parse(dataset);
    checkArgument(records.size() < maxRecords,
        "Amount of records can not be more than " + maxRecords);

    var datasetId = datasetService.createDataset(datasetName, country, language, records);
    return new DatasetIdDto(datasetId);
  }

  @ApiOperation("Get dataset progress information")
  @GetMapping(value = "dataset/{id}", produces = APPLICATION_JSON_VALUE)
  public DatasetInfoDto retrieveDataset(
      @ApiParam(value = "id of the dataset", required = true) @PathVariable("id") String datasetId) {
    var error1 = new ErrorInfoDto("record failed with some exception", List.of("1","2"));
    var error2 = new ErrorInfoDto("record failed with some exception", List.of("3", "4"));
    var report1 = new ReportByStepDto(Step.VALIDATE_EXTERNAL, List.of(error1, error2));
    var report2 = new ReportByStepDto(Step.ENRICH, List.of(error1, error2));
    return new DatasetInfoDto("Will come in the future", List.of(report1, report2));
  }
}
