package eu.europeana.metis.sandbox.controller;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.schema.convert.RdfConversionUtils;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.view.DatasetProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemPattern;
import io.swagger.annotations.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/pattern-analysis/")
@Api(value = "Pattern Analysis Controller")
public class PatternAnalysisController {

    private final PatternAnalysisService<Step> patternAnalysisService;
    private final RdfConversionUtils rdfConversionUtils = new RdfConversionUtils();

    public PatternAnalysisController(PatternAnalysisService<Step> patternAnalysisService){
        this.patternAnalysisService = patternAnalysisService;
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
     * @return
     * @throws SerializationException
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
            @RequestBody String record) throws SerializationException {
        return patternAnalysisService.getRecordPatternAnalysis(datasetId, executionStep, executionTimestamp, rdfConversionUtils.convertStringToRdf(record));

    }
}
