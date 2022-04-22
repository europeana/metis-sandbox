package eu.europeana.metis.sandbox.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.service.problempatterns.ExecutionPointService;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.schema.convert.RdfConversionUtils;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.view.DatasetProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemPattern;
import io.swagger.annotations.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/pattern-analysis/")
@Api(value = "Pattern Analysis Controller")
public class PatternAnalysisController {

    private final PatternAnalysisService<Step> patternAnalysisService;
    private final ExecutionPointService executionPointService;
    private final RecordLogService recordLogService;
    private final RdfConversionUtils rdfConversionUtils = new RdfConversionUtils();

    public PatternAnalysisController(PatternAnalysisService<Step> patternAnalysisService,
                                     ExecutionPointService executionPointService,
                                     RecordLogService recordLogService){
        this.patternAnalysisService = patternAnalysisService;
        this.executionPointService = executionPointService;
        this.recordLogService = recordLogService;
    }

    /**
     * Retrieves the pattern analysis from a given dataset
     *
     * @param datasetId The id of the dataset to gather the pattern analysis
     * @param executionTimestamp The timestamp of when the analysis was executed
     * @return The pattern analysis of the dataset
     */
    @ApiOperation("Retrieve pattern analysis from a dataset")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Response contains the pattern analysis"),
            @ApiResponse(code = 404, message = "Dataset not found")
    })
    @GetMapping(value = "{id}/get-dataset-pattern-analysis", produces = APPLICATION_JSON_VALUE)
    public DatasetProblemPatternAnalysisView<Step> getDatasetPatternAnalysis(
            @ApiParam(value = "id of the dataset", required = true) @PathVariable("id") String datasetId,
            @ApiParam(value = "timestamp of when the step was executed", required = true) @RequestParam LocalDateTime executionTimestamp){
        return new DatasetProblemPatternAnalysisView<>(patternAnalysisService.getDatasetPatternAnalysis(datasetId, Step.VALIDATE_INTERNAL, executionTimestamp).orElse(
                new DatasetProblemPatternAnalysis<>("0", null, null, new ArrayList<>())));

    }

    /**
     * Retrieved the pattern analysis from a given record
     *
     * @param datasetId The id of the dataset that the record belongs to
     * @param recordId The record content as a String
     * @return A list with pattern problems that the record contains
     * @throws SerializationException if there's an issue with the record content
     */
    @ApiOperation("Retrieve pattern analysis from a specific record")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Response contains the pattern analysis"),
            @ApiResponse(code = 404, message = "Not able to retrieve the pattern analysis of record")
    })
    @GetMapping(value = "{id}/get-record-pattern-analysis", produces = APPLICATION_JSON_VALUE)
    public List<ProblemPattern> getRecordPatternAnalysis(
            @ApiParam(value = "id of the dataset", required = true) @PathVariable("id") String datasetId,
            @ApiParam(value = "The record content as a file", required = true) @RequestParam String recordId) throws SerializationException {
        String recordContent = recordLogService.getRecordLogEntity(recordId, datasetId, Step.VALIDATE_INTERNAL).getContent();
        return patternAnalysisService.getRecordPatternAnalysis(rdfConversionUtils.convertStringToRdf(recordContent));

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

    private static class DatasetProblemPatternAnalysisView<T> {
        @JsonProperty
        private final String datasetId;
        @JsonProperty
        private final T executionStep;
        @JsonProperty
        private final String executionTimestamp;
        @JsonProperty
        private final List<ProblemPattern> problemPatternList;

        private DatasetProblemPatternAnalysisView( DatasetProblemPatternAnalysis<T> datasetProblemPatternAnalysis) {
            this.datasetId = datasetProblemPatternAnalysis.getDatasetId();
            this.executionStep = datasetProblemPatternAnalysis.getExecutionStep();
            this.executionTimestamp = datasetProblemPatternAnalysis.getExecutionTimestamp() == null ? null :
        datasetProblemPatternAnalysis.getExecutionTimestamp().toString();
            this.problemPatternList = datasetProblemPatternAnalysis.getProblemPatternList();
        }
    }
}
