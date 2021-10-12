package eu.europeana.metis.sandbox.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.DatasetIdDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.util.ZipService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

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

  private final ZipService zipService;
  private final DatasetService datasetService;
  private final DatasetReportService reportService;

  public DatasetController(ZipService zipService,
      DatasetService datasetService,
      DatasetReportService reportService) {
    this.zipService = zipService;
    this.datasetService = datasetService;
    this.reportService = reportService;
  }

  @ApiOperation("Process the given dataset")
  @ApiResponses({
          @ApiResponse(code = 202, message = MESSAGE_FOR_PROCESS_DATASET, response = Object.class)
  })
  @PostMapping(value = "dataset/{name}/process", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public DatasetIdDto processDataset(
      @ApiParam(value = "name of the dataset", required = true) @PathVariable(value = "name") String datasetName,
      @ApiParam(value = "country of the dataset", required = true, defaultValue = "Netherlands") @RequestParam Country country,
      @ApiParam(value = "language of the dataset", required = true, defaultValue = "nl") @RequestParam Language language,
      @ApiParam(value = "dataset records uploaded in a zip file", required = false) @RequestParam MultipartFile dataset,
      @ApiParam(value = "dataset records URL to download in a zip file", required = false) @RequestParam String URL) {
    checkArgument(namePattern.matcher(datasetName).matches(),
        "dataset name can only include letters, numbers, _ or - characters");
    List<ByteArrayInputStream> records = new ArrayList<>();
    if (dataset != null && !dataset.isEmpty()) {
      records = zipService.parse(dataset);
    } else if (!URL.isEmpty()){
      records = zipService.parse(URL);
    }
    checkArgument(records.size() < maxRecords,
        "Amount of records can not be more than " + maxRecords);

    // When saving the record into the database, the variable 'language' is saved as a 2-letter code
    var datasetObject = datasetService.createDataset(datasetName, country, language, records);
    return new DatasetIdDto(datasetObject);
  }

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
  public List<LanguageView> getAllLanguages(){
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
