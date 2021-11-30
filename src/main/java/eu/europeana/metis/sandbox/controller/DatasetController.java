package eu.europeana.metis.sandbox.controller;

import static com.google.common.base.Preconditions.checkArgument;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.dto.DatasetIdDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.workflow.HarvestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/")
@Api(value = "Dataset Controller")
class DatasetController {

  private static final String MESSAGE_FOR_PROCESS_DATASET = ""
      + "<span style=\"font-style: normal; font-size: 125%; font-weight: 750;\">"
      + "202 Accepted</span>"
      + " - The response body will contain an object of type"
      + " <span style=\"font-style: normal; font-size: 125%; font-weight: 750;\">"
      + DatasetIdDto.SWAGGER_MODEL_NAME + "</span>.";

  private static final String MESSAGE_FOR_RETRIEVE_DATASET = ""
      + "<span style=\"font-style: normal; font-size: 125%; font-weight: 750;\">"
      + "200 OK</span>"
      + " - The response body will contain an object of type"
      + " <span style=\"font-style: normal; font-size: 125%; font-weight: 750;\">"
      + ProgressInfoDto.PROGRESS_SWAGGER_MODEL_NAME + "</span>.";

  private static final Pattern namePattern = Pattern.compile("[a-zA-Z0-9_-]+");

  @Value("${sandbox.dataset.max-size}")
  private int maxRecords;

  private final HarvestService harvestService;
  private final DatasetService datasetService;
  private final DatasetReportService reportService;

  public DatasetController(HarvestService harvestService,
      DatasetService datasetService,
      DatasetReportService reportService) {
    this.harvestService = harvestService;
    this.datasetService = datasetService;
    this.reportService = reportService;
  }

  /**
   * POST API call for harvesting and processing the records given a zip file
   * @param datasetName The given name of the dataset to be processed
   * @param country The given country from which the records refer to
   * @param language The given language that the records contain
   * @param dataset The given dataset itself to be processed as a zip file
   * @return 202 if it's processed correctly, 4xx or 500 otherwise
   */
  @ApiOperation("Process the given dataset by HTTP providing a file")
  @ApiResponses({
      @ApiResponse(code = 202, message = MESSAGE_FOR_PROCESS_DATASET, response = Object.class)
  })
  @PostMapping(value = "dataset/{name}/harvestByFile", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public DatasetIdDto harvestDatasetFromFile(
      @ApiParam(value = "name of the dataset", required = true) @PathVariable(value = "name") String datasetName,
      @ApiParam(value = "country of the dataset", required = true, defaultValue = "Netherlands") @RequestParam Country country,
      @ApiParam(value = "language of the dataset", required = true, defaultValue = "nl") @RequestParam Language language,
      @ApiParam(value = "dataset records uploaded in a zip file", required = true) @RequestParam MultipartFile dataset,
      @ApiParam(value = "xslt file to transform to EDM external") @RequestParam(required = false) MultipartFile xsltFile)
      throws IOException {
    checkArgument(namePattern.matcher(datasetName).matches(),
        "dataset name can only include letters, numbers, _ or - characters");

    List<ByteArrayInputStream> records = harvestService.harvest(dataset);

    checkArgument(records.size() < maxRecords,
        "Amount of records can not be more than " + maxRecords);


    String xsltFileString = "";

    if(xsltFile != null && !xsltFile.isEmpty()){
      checkArgument(xsltFile.getContentType().matches("text/xml"),
          "The given xslt file should be a single xml file.");
      xsltFileString = new String(xsltFile.getBytes(), StandardCharsets.UTF_8);
    }

    // When saving the record into the database, the variable 'language' is saved as a 2 or 3-letter code
    Dataset datasetObject = datasetService.createDataset(datasetName, country, language, records,
        xsltFileString);
    return new DatasetIdDto(datasetObject);
  }

  /**
   * POST API call for harvesting and processing the records given a URL of a zip file
   * @param datasetName The given name of the dataset to be processed
   * @param country The given country from which the records refer to
   * @param language The given language that the records contain
   * @param url The given dataset itself to be processed as a URL of a zip file
   * @return 202 if it's processed correctly, 4xx or 500 otherwise
   */
  @ApiOperation("Process the given dataset by HTTP providing an URL")
  @ApiResponses({
      @ApiResponse(code = 202, message = MESSAGE_FOR_PROCESS_DATASET, response = Object.class)
  })
  @PostMapping(value = "dataset/{name}/harvestByUrl", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public DatasetIdDto harvestDatasetFromURL(
      @ApiParam(value = "name of the dataset", required = true) @PathVariable(value = "name") String datasetName,
      @ApiParam(value = "country of the dataset", required = true, defaultValue = "Netherlands") @RequestParam Country country,
      @ApiParam(value = "language of the dataset", required = true, defaultValue = "nl") @RequestParam Language language,
      @ApiParam(value = "dataset records URL to download in a zip file", required = true) @RequestParam String url,
      @ApiParam(value = "xslt file to transform to EDM external") @RequestParam(required = false) String xsltFile) {
    checkArgument(namePattern.matcher(datasetName).matches(),
        "dataset name can only include letters, numbers, _ or - characters");
    List<ByteArrayInputStream> records = harvestService.harvest(url);

    checkArgument(records.size() < maxRecords,
        "Amount of records can not be more than " + maxRecords);
    Dataset datasetObject = datasetService.createDataset(datasetName, country, language, records,
        xsltFile);
    return new DatasetIdDto(datasetObject);
  }

  /**
   * GET API call to return the progress status of a given dataset id
   * @param datasetId The given dataset id to look for
   * @return The report of the dataset status
   */
  @ApiOperation("Get dataset progress information")
  @ApiResponses({
      @ApiResponse(code = 200, message = MESSAGE_FOR_RETRIEVE_DATASET, response = Object.class)
  })
  @GetMapping(value = "dataset/{id}", produces = APPLICATION_JSON_VALUE)
  public ProgressInfoDto retrieveDataset(
      @ApiParam(value = "id of the dataset", required = true) @PathVariable("id") String datasetId) {
    return reportService.getReport(datasetId);
  }

  /**
   * Get all available countries that can be used.
   * <p>The list is retrieved based on an internal enum</p>
   *
   * @return The list of countries available
   */
  @ApiOperation("Get data of all available countries")
  @ApiResponses({
      @ApiResponse(code = 200, message = MESSAGE_FOR_RETRIEVE_DATASET, response = Object.class)
  })
  @GetMapping(value = "dataset/countries", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<CountryView> getAllCountries() {
    return Arrays.stream(Country.values()).map(CountryView::new).collect(Collectors.toList());
  }

  /**
   * Get all available languages that can be used.
   * <p>The list is retrieved based on an internal enum</p>
   *
   * @return The list of languages that are available
   */
  @ApiOperation("Get data of all available languages")
  @ApiResponses({
      @ApiResponse(code = 200, message = MESSAGE_FOR_RETRIEVE_DATASET, response = Object.class)
  })
  @GetMapping(value = "dataset/languages", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<LanguageView> getAllLanguages() {
    return Language.getLanguageListSortedByName().stream().map(LanguageView::new)
        .collect(Collectors.toList());
  }

  private static class CountryView {

    @JsonProperty("name")
    private final String name;
    @JsonProperty("xmlValue")
    private final String xmlValue;

    CountryView(Country country) {
      this.name = country.name();
      this.xmlValue = country.xmlValue();
    }
  }

  private static class LanguageView {

    @JsonProperty("name")
    private final String name;
    @JsonProperty("xmlValue")
    private final String xmlValue;

    LanguageView(Language language) {
      this.name = language.name();
      this.xmlValue = language.xmlValue();
    }
  }
}
