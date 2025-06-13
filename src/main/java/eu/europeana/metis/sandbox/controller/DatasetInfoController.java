package eu.europeana.metis.sandbox.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressInfoDTO;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dataset/")
@Tag(name = "Dataset Info Controller")
public class DatasetInfoController {

  private final DatasetReportService datasetReportService;

  public DatasetInfoController(DatasetReportService datasetReportService) {
    this.datasetReportService = datasetReportService;
  }

  /**
   * GET API calls to return the progress status of a given dataset id
   *
   * @param datasetId The given dataset id to look for
   * @return The report of the dataset status
   */
  @Operation(summary = "Get dataset's progress", description = "Get dataset progress information")
  @ApiResponse(responseCode = "200")
  @ApiResponse(responseCode = "400")
  @GetMapping(value = "{id}/progress", produces = APPLICATION_JSON_VALUE)
  public ExecutionProgressInfoDTO getDatasetProgress(
      @Parameter(description = "id of the dataset", required = true) @PathVariable("id") String datasetId) {
    //TODO 24-02-2022: We need to update the type of info encapsulate in this object. The number of duplicated record is missing for example
    //        return reportService.getReport(datasetId);
    return datasetReportService.getProgress(datasetId);
  }

  /**
   * GET API calls to return the information about a given dataset id
   *
   * @param datasetId The given dataset id to look for
   * @return The report of the dataset status
   */
  @Operation(summary = "Get dataset information", description = "Get dataset information")
  @ApiResponse(responseCode = "200")
  @ApiResponse(responseCode = "400")
  @GetMapping(value = "{id}/info", produces = APPLICATION_JSON_VALUE)
  public DatasetInfoDTO getDatasetInfo(
      @Parameter(description = "id of the dataset", required = true) @PathVariable("id") String datasetId) {
    return datasetReportService.getDatasetInfo(datasetId);
  }


  /**
   * Get all available countries that can be used.
   * <p>The list is retrieved based on an internal enum</p>
   *
   * @return The list of countries available
   */
  @Operation(description = "Get data of all available countries")
  @ApiResponse(responseCode = "200", description = "List containing all available countries", content = {
      @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = CountryView.class))})
  @ApiResponse(responseCode = "400")
  @GetMapping(value = "countries", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public List<CountryView> getAllCountries() {
    return Country.getCountryListSortedByName().stream().map(CountryView::new).toList();
  }

  /**
   * Get all available languages that can be used.
   * <p>The list is retrieved based on an internal enum</p>
   *
   * @return The list of languages that are available
   */
  @Operation(description = "Get data of all available languages")
  @ApiResponse(responseCode = "200", description = "List containing all available languages", content = {
      @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = LanguageView.class))})
  @ApiResponse(responseCode = "400")
  @GetMapping(value = "languages", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public List<LanguageView> getAllLanguages() {
    return Language.getLanguageListSortedByName().stream().map(LanguageView::new).toList();
  }

  //todo: This is a copy of the metis-core class, once the metis-core is used as orchestrator this class should be removed.
  public static class CountryView {

    @JsonProperty("name")
    private final String name;
    @JsonProperty("xmlValue")
    private final String xmlValue;

    /**
     * Instantiates a new Country view.
     *
     * @param country the country
     */
    CountryView(Country country) {
      this.name = country.name();
      this.xmlValue = country.xmlValue();
    }
  }

  //todo: This is a copy of the metis-core class, once the metis-core is used as orchestrator this class should be removed.
  public static class LanguageView {

    @JsonProperty("name")
    private final String name;
    @JsonProperty("xmlValue")
    private final String xmlValue;

    /**
     * Instantiates a new Language view.
     *
     * @param language the language
     */
    LanguageView(Language language) {
      this.name = language.name();
      this.xmlValue = language.xmlValue();
    }
  }
}
