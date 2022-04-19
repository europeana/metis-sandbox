package eu.europeana.metis.sandbox.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.service.problempatterns.ExecutionPointService;
import eu.europeana.metis.schema.convert.RdfConversionUtils;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.view.DatasetProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemPattern;
import io.swagger.annotations.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/pattern-analysis/")
@Api(value = "Pattern Analysis Controller")
public class PatternAnalysisController {

    private final PatternAnalysisService<Step> patternAnalysisService;
    private final ExecutionPointService executionPointService;
    private final RdfConversionUtils rdfConversionUtils = new RdfConversionUtils();

    public PatternAnalysisController(PatternAnalysisService<Step> patternAnalysisService,
                                     ExecutionPointService executionPointService){
        this.patternAnalysisService = patternAnalysisService;
        this.executionPointService = executionPointService;
    }

    /**
     * Retrieves the pattern analysis from a given dataset
     * @param datasetId The id of the dataset to gather the pattern analysis
     * @param executionStep The execution step of dataset processing to gather the analysis from
     * @param executionTimestamp The timestamp of when the step was executed
     * @return The pattern analysis of the dataset
     */
    @ApiOperation("Retrieve pattern analysis from a dataset")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Response contains the pattern analysis"),
            @ApiResponse(code = 404, message = "Dataset not found")
    })
    @GetMapping(value = "{id}/get-dataset-pattern-analysis", produces = APPLICATION_JSON_VALUE)
    public DatasetProblemPatternAnalysis<Step> getDatasetPatternAnalysis(
            @ApiParam(value = "id of the dataset", required = true) @PathVariable("id") String datasetId,
            @ApiParam(value = "execution step of dataset processing to gather the analysis from", required = true) @RequestParam Step executionStep,
            @ApiParam(value = "timestamp of when the step was executed", required = true) @RequestParam LocalDateTime executionTimestamp){
        return patternAnalysisService.getDatasetPatternAnalysis(datasetId, executionStep, executionTimestamp).orElse(null);

    }

    /**
     * Retrieved the pattern analysis from a given record
     * @param datasetId The id of the dataset the record belongs to
     * @param executionStep The execution step of dataset processing to gather the analysis from
     * @param executionTimestamp The timestamp of when the step was executed
     * @param record The record content as a String
     * @return A list with pattern problems that the record contains
     * @throws SerializationException if there's an issue with the record content
     * @throws IOException if an issue occurs while processing the record's content
     */
    @ApiOperation("Retrieve pattern analysis from a specific record")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Response contains the pattern analysis"),
            @ApiResponse(code = 404, message = "Not able to retrieve the pattern analysis of record")
    })
    @GetMapping(value = "{id}/get-record-pattern-analysis", produces = APPLICATION_JSON_VALUE)
    public List<ProblemPattern> getRecordPatternAnalysis(
            @ApiParam(value = "id of the dataset", required = true) @PathVariable("id") String datasetId,
            @ApiParam(value = "execution step of dataset processing to gather the analysis from", required = true) @RequestParam Step executionStep,
            @ApiParam(value = "timestamp of when the step was executed", required = true) @RequestParam LocalDateTime executionTimestamp,
            @ApiParam(value = "The record content as a file") @RequestParam(required = false) MultipartFile record) throws SerializationException, IOException {
        return patternAnalysisService.getRecordPatternAnalysis(datasetId, executionStep, executionTimestamp,
                rdfConversionUtils.convertInputStreamToRdf(record.getInputStream()));

    }

    /**
     * Get all available dataset processing steps values.
     * <p>The list is retrieved based on an internal enum</p>
     *
     * @return The list of dataset processing steps that are available
     */
    @ApiOperation("Get data of all available dataset processing steps")
    @ApiResponses({
            @ApiResponse(code = 200, message = "All values retrieved", response = Object.class),
            @ApiResponse(code = 404, message = "Not able to retrieve all step values")
    })
    @GetMapping(value = "steps", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<StepView> getAllSteps() {
        return Arrays.stream(Step.values()).map(PatternAnalysisController.StepView::new).collect(Collectors.toList());
    }

    /**
     * Get all available execution timestamps available in the database.
     * <p>The list is retrieved based on database entry value</p>
     *
     * @return The set of execution timestamp that are available
     */
    @ApiOperation("Get data of all available execution timestamps")
    @ApiResponses({
            @ApiResponse(code = 200, message = "All values retrieved", response = Object.class),
            @ApiResponse(code = 404, message = "Not able to retrieve all timestamps values")
    })
    @GetMapping(value = "execution-timestamps", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Set<LocalDateTime> getAllExecutionTimestamps() {
        return executionPointService.getAllExecutionTimestamps();
    }

    private static class StepView {

        @JsonProperty("name")
        private final String enumName;
        @JsonProperty("value")
        private final String enumValue;

        StepView(Step step) {
            this.enumName = step.name();
            this.enumValue = step.value();
        }
    }
}
