package eu.europeana.metis.sandbox.controller;

import static com.google.common.base.Preconditions.checkArgument;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.metis.sandbox.common.HarvestContent;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.common.exception.XsltProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.dto.DatasetIdDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.service.dataset.AsyncDatasetPublishService;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService;
import eu.europeana.metis.sandbox.service.workflow.HarvestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
@RequestMapping("/dataset/")
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


  private final HarvestService harvestService;
  private final DatasetService datasetService;
  private final DatasetReportService reportService;
  private final RecordLogService recordLogService;
  private final RecordTierCalculationService recordTierCalculationService;
  private final AsyncDatasetPublishService asyncDatasetPublishService;

  public DatasetController(HarvestService harvestService,
      DatasetService datasetService,
      DatasetReportService reportService,
      RecordLogService recordLogService,
      RecordTierCalculationService recordTierCalculationService,
      AsyncDatasetPublishService asyncDatasetPublishService) {
    this.harvestService = harvestService;
    this.datasetService = datasetService;
    this.reportService = reportService;
    this.recordLogService = recordLogService;
    this.recordTierCalculationService = recordTierCalculationService;
    this.asyncDatasetPublishService = asyncDatasetPublishService;
  }

  /**
   * POST API calls for harvesting and processing the records given a zip file
   *
   * @param datasetName the given name of the dataset to be processed
   * @param country     the given country from which the records refer to
   * @param language    the given language that the records contain
   * @param dataset     the given dataset itself to be processed as a zip file
   * @param xsltFile    the xslt file used for transformation to edm external
   * @return 202 if it's processed correctly, 4xx or 500 otherwise
   */
  @ApiOperation("Process the given dataset by HTTP providing a file")
  @ApiResponses({
      @ApiResponse(code = 202, message = MESSAGE_FOR_PROCESS_DATASET, response = Object.class)
  })
  @PostMapping(value = "{name}/harvestByFile", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public DatasetIdDto harvestDatasetFromFile(
      @ApiParam(value = "name of the dataset", required = true) @PathVariable(value = "name") String datasetName,
      @ApiParam(value = "country of the dataset", required = true, defaultValue = "Netherlands") @RequestParam Country country,
      @ApiParam(value = "language of the dataset", required = true, defaultValue = "Dutch") @RequestParam Language language,
      @ApiParam(value = "dataset records uploaded in a zip file", required = true) @RequestParam MultipartFile dataset,
      @ApiParam(value = "xslt file to transform to EDM external") @RequestParam(required = false) MultipartFile xsltFile) {
    checkArgument(namePattern.matcher(datasetName).matches(),
        "dataset name can only include letters, numbers, _ or - characters");

    HarvestContent harvestedRecords =
        harvestService.harvestZipMultipartFile(dataset);

    InputStream xsltFileString = createXsltAsInputStreamIfPresent(xsltFile);

    // When saving the record into the database, the variable 'language' is saved as a 2 or 3-letter code
    Dataset datasetObject = datasetService.createDataset(datasetName, country, language,
        harvestedRecords.getContent(),
        harvestedRecords.hasReachedRecordLimit(), xsltFileString);

    return new DatasetIdDto(datasetObject);
  }


  /**
   * POST API calls for harvesting and processing the records given a URL of a zip file
   *
   * @param datasetName the given name of the dataset to be processed
   * @param country     the given country from which the records refer to
   * @param language    the given language that the records contain
   * @param url         the given dataset itself to be processed as a URL of a zip file
   * @param xsltFile    the xslt file used for transformation to edm external
   * @return 202 if it's processed correctly, 4xx or 500 otherwise
   */
  @ApiOperation("Process the given dataset by HTTP providing an URL")
  @ApiResponses({
      @ApiResponse(code = 202, message = MESSAGE_FOR_PROCESS_DATASET, response = Object.class)
  })
  @PostMapping(value = "{name}/harvestByUrl", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public DatasetIdDto harvestDatasetFromURL(
      @ApiParam(value = "name of the dataset", required = true) @PathVariable(value = "name") String datasetName,
      @ApiParam(value = "country of the dataset", required = true, defaultValue = "Netherlands") @RequestParam Country country,
      @ApiParam(value = "language of the dataset", required = true, defaultValue = "Dutch") @RequestParam Language language,
      @ApiParam(value = "dataset records URL to download in a zip file", required = true) @RequestParam String url,
      @ApiParam(value = "xslt file to transform to EDM external") @RequestParam(required = false) MultipartFile xsltFile) {

    checkArgument(namePattern.matcher(datasetName).matches(),
        "dataset name can only include letters, numbers, _ or - characters");
    HarvestContent harvestedRecords = harvestService.harvestZipUrl(url);

    InputStream xsltInputStream = createXsltAsInputStreamIfPresent(xsltFile);

    Dataset datasetObject = datasetService.createDataset(datasetName, country, language,
        harvestedRecords.getContent(), harvestedRecords.hasReachedRecordLimit(), xsltInputStream);
    return new DatasetIdDto(datasetObject);
  }

  /**
   * POST API calls for harvesting and processing the records given a URL of an OAI-PMH endpoint
   *
   * @param datasetName    the given name of the dataset to be processed
   * @param country        the given country from which the records refer to
   * @param language       the given language that the records contain
   * @param url            the given URL of the OAI-PMH repository to be processed
   * @param setspec        forms a unique identifier for the set within the repository, it must be
   *                       unique for each set.
   * @param metadataformat or metadata prefix is a string to specify the metadata format in OAI-PMH
   *                       requests issued to the repository
   * @param xsltFile       the xslt file used for transformation to edm external
   * @return 202 if it's processed correctly, 4xx or 500 otherwise
   */
  @ApiOperation("Process the given dataset using OAI-PMH")
  @ApiResponses({
      @ApiResponse(code = 202, message = MESSAGE_FOR_PROCESS_DATASET, response = Object.class)
  })
  @PostMapping(value = "{name}/harvestOaiPmh", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public DatasetIdDto harvestDatasetOaiPmh(
      @ApiParam(value = "name of the dataset", required = true) @PathVariable(value = "name") String datasetName,
      @ApiParam(value = "country of the dataset", required = true, defaultValue = "Netherlands") @RequestParam Country country,
      @ApiParam(value = "language of the dataset", required = true, defaultValue = "Dutch") @RequestParam Language language,
      @ApiParam(value = "dataset URL records", required = true) @RequestParam String url,
      @ApiParam(value = "dataset specification", required = true) @RequestParam String setspec,
      @ApiParam(value = "metadata format") @RequestParam String metadataformat,
      @ApiParam(value = "xslt file to transform to EDM external") @RequestParam(required = false) MultipartFile xsltFile) {
    checkArgument(namePattern.matcher(datasetName).matches(),
        "dataset name can only include letters, numbers, _ or - characters");


    InputStream xsltInputStream = createXsltAsInputStreamIfPresent(xsltFile);
    String createdDatasetId = datasetService.createEmptyDataset(datasetName, country, language, xsltInputStream);

    asyncDatasetPublishService.harvestOaiPmh(datasetName, createdDatasetId, country, language, xsltInputStream, url, setspec, metadataformat);

    return new DatasetIdDto(new Dataset(createdDatasetId, Collections.emptySet(), 0));
  }

  /**
   * GET API calls to return the progress status of a given dataset id
   *
   * @param datasetId The given dataset id to look for
   * @return The report of the dataset status
   */
  @ApiOperation("Get dataset progress information")
  @ApiResponses({
      @ApiResponse(code = 200, message = MESSAGE_FOR_RETRIEVE_DATASET, response = Object.class)
  })
  @GetMapping(value = "{id}", produces = APPLICATION_JSON_VALUE)
  public ProgressInfoDto getDataset(
      @ApiParam(value = "id of the dataset", required = true) @PathVariable("id") String datasetId) {
    return reportService.getReport(datasetId);
  }

  /**
   * GET API returns the generated tier calculation view for a stored record.
   *
   * @param datasetId the dataset id
   * @param recordId  the record id
   * @return the record tier calculation view
   * @throws NoRecordFoundException if record was not found
   */
  @ApiOperation("Get the tier calculation")
  @ApiResponses({
      @ApiResponse(code = 200, message = "Response contains the tier calculation view"),
      @ApiResponse(code = 404, message = "Record not found")
  })
  @GetMapping(value = "{id}/record/compute-tier-calculation", produces = APPLICATION_JSON_VALUE)
  public RecordTierCalculationView computeRecordTierCalculation(
      @PathVariable("id") String datasetId,
      @RequestParam String recordId) throws NoRecordFoundException {
    return recordTierCalculationService.calculateTiers(recordId, datasetId);
  }

  /**
   * GET API returns the string representation of the stored record.
   *
   * @param datasetId the dataset id
   * @param recordId  the record id
   * @return the string representation of the stored record
   * @throws NoRecordFoundException if record was not found
   */
  @ApiOperation("Get record string representation")
  @ApiResponses({
      @ApiResponse(code = 200, message = "String representation of record"),
      @ApiResponse(code = 404, message = "Record not found")
  })
  @GetMapping(value = "{id}/record")
  public String getRecord(@PathVariable("id") String datasetId, @RequestParam String recordId)
      throws NoRecordFoundException {
    return recordLogService.getProviderRecordString(recordId, datasetId);
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
  @GetMapping(value = "countries", produces = APPLICATION_JSON_VALUE)
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
  @GetMapping(value = "languages", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<LanguageView> getAllLanguages() {
    return Language.getLanguageListSortedByName().stream().map(LanguageView::new)
        .collect(Collectors.toList());
  }

  private InputStream createXsltAsInputStreamIfPresent(MultipartFile xslt) {
    if (xslt != null && !xslt.isEmpty()) {
      checkArgument(Objects.requireNonNull(xslt.getContentType()).contains("xml") ||
              xslt.getContentType().contains("xslt"),
          "The given xslt file should be a single xml file.");
      try {
        return new ByteArrayInputStream(xslt.getBytes());
      } catch (IOException e) {
        throw new XsltProcessingException("Something wrong happened while processing xslt file.",
            e);
      }
    }
    return new ByteArrayInputStream(new byte[0]);
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
