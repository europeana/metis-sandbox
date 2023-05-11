package eu.europeana.metis.sandbox.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto.Status;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.problempatterns.ExecutionPointService;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.schema.convert.RdfConversionUtils;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.patternanalysis.view.DatasetProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemOccurrence;
import eu.europeana.patternanalysis.view.ProblemPattern;
import eu.europeana.patternanalysis.view.ProblemPatternDescription.ProblemPatternId;
import eu.europeana.patternanalysis.view.RecordAnalysis;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Pattern analysis controller
 */
@RestController
@RequestMapping("/pattern-analysis/")
@Tag(name = "Pattern Analysis Controller")
public class PatternAnalysisController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatternAnalysisController.class);

    private final PatternAnalysisService<Step, ExecutionPoint> patternAnalysisService;
    private final ExecutionPointService executionPointService;
    private final RecordLogService recordLogService;
    private final DatasetReportService datasetReportService;
    private final RdfConversionUtils rdfConversionUtils = new RdfConversionUtils();
    private final Map<String, Lock> datasetIdLocksMap = new ConcurrentHashMap<>();
    private final LockRegistry lockRegistry;

    /**
     * Constructor with required parameters.
     *
     * @param patternAnalysisService the pattern analysis service
     * @param executionPointService  the execution point service
     * @param recordLogService       the record log service
     * @param datasetReportService   the dataset report service
     * @param lockRegistry           the lock registry
     */
    public PatternAnalysisController(PatternAnalysisService<Step, ExecutionPoint> patternAnalysisService,
                                     ExecutionPointService executionPointService,
                                     RecordLogService recordLogService, DatasetReportService datasetReportService,
                                     LockRegistry lockRegistry) {
        this.patternAnalysisService = patternAnalysisService;
        this.executionPointService = executionPointService;
        this.recordLogService = recordLogService;
        this.datasetReportService = datasetReportService;
        this.lockRegistry = lockRegistry;
    }

    /**
     * Retrieves the pattern analysis from a given dataset
     *
     * @param datasetId The id of the dataset to gather the pattern analysis
     * @return The pattern analysis of the dataset
     */
    @Operation(description = "Retrieve pattern analysis from a dataset")
    @ApiResponse(responseCode = "200", description = "Response contains the pattern analysis")
    @ApiResponse(responseCode = "404", description = "Dataset not found")
    @GetMapping(value = "{id}/get-dataset-pattern-analysis", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<DatasetProblemPatternAnalysisView<Step>> getDatasetPatternAnalysis(
            @Parameter(description = "id of the dataset", required = true) @PathVariable("id") String datasetId) {
        Optional<ExecutionPoint> datasetExecutionPointOptional = executionPointService.getExecutionPoint(datasetId,
                Step.VALIDATE_INTERNAL.toString());

        return datasetExecutionPointOptional.flatMap(executionPoint -> {
                    finalizeDatasetPatternAnalysis(datasetId, executionPoint);
                    return patternAnalysisService.getDatasetPatternAnalysis(
                            datasetId, Step.VALIDATE_INTERNAL, datasetExecutionPointOptional.get().getExecutionTimestamp());
                }).map(analysis -> new ResponseEntity<>(new DatasetProblemPatternAnalysisView<>(analysis), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(
                        DatasetProblemPatternAnalysisView.getEmptyDatasetProblemPatternAnalysisView(),
                        HttpStatus.NOT_FOUND));
    }

    private void finalizeDatasetPatternAnalysis(String datasetId, ExecutionPoint datasetExecutionPoint) {
        if (datasetReportService.getReport(datasetId).getStatus() == Status.COMPLETED) {
            final Lock lock = datasetIdLocksMap.computeIfAbsent(datasetId, s -> lockRegistry.obtain("finalizePatternAnalysis_" + datasetId));
            try {
                lock.lock();
                LOGGER.debug("Finalize analysis: {} lock, Locked", datasetId);
                patternAnalysisService.finalizeDatasetPatternAnalysis(datasetExecutionPoint);
            } catch (PatternAnalysisException e) {
                LOGGER.error("Something went wrong during finalizing pattern analysis", e);
            } finally {
                lock.unlock();
                LOGGER.debug("Finalize analysis: {} lock, Unlocked", datasetId);
            }
        }
    }

    /**
     * Retrieved the pattern analysis from a given record
     *
     * @param datasetId The id of the dataset that the record belongs to
     * @param recordId  The record content as a String
     * @return A list with pattern problems that the record contains
     * @throws SerializationException if there's an issue with the record content
     */
    @Operation(description = "Retrieve pattern analysis from a specific record")
    @ApiResponse(responseCode = "200", description = "Response contains the pattern analysis")
    @ApiResponse(responseCode = "404", description = "Not able to retrieve the pattern analysis of record")
    @GetMapping(value = "{id}/get-record-pattern-analysis", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProblemPattern>> getRecordPatternAnalysis(
            @Parameter(description = "id of the dataset", required = true) @PathVariable("id") String datasetId,
            @Parameter(description = "The record content as a file", required = true) @RequestParam String recordId)
            throws SerializationException {
        RecordLogEntity recordLog = recordLogService.getRecordLogEntity(recordId, datasetId, Step.VALIDATE_INTERNAL);
        return recordLog == null ? new ResponseEntity<>(HttpStatus.NOT_FOUND) :
                new ResponseEntity<>(
                        patternAnalysisService.getRecordPatternAnalysis(rdfConversionUtils.convertStringToRdf(recordLog.getContent()))
                                .stream()
                                .map(DatasetProblemPatternAnalysisFilter::cleanMessageReportForP7TitleIsEnough)
                                .map(DatasetProblemPatternAnalysisFilter::sortRecordAnalysisByRecordId)
                                .sorted(Comparator.comparing(problemPattern -> problemPattern.getProblemPatternDescription().getProblemPatternId()))
                                .collect(Collectors.toList()),
                        HttpStatus.OK);
    }

    /**
     * Get all available execution timestamps available in the database.
     * <p>The list is retrieved based on database entry value</p>
     *
     * @return The set of execution timestamp that are available
     */
    @Operation(description = "Get data of all available execution timestamps")
    @ApiResponse(responseCode = "200", description = "All values retrieved")
    @ApiResponse(responseCode = "404", description = "Not able to retrieve all timestamps values")
    @GetMapping(value = "execution-timestamps", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Set<LocalDateTime> getAllExecutionTimestamps() {
        return executionPointService.getAllExecutionTimestamps();
    }

    /**
     * Evict cache items every day.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    private void cleanCache() {
        for (Entry<String, Lock> entry : datasetIdLocksMap.entrySet()) {
            final Lock lock = entry.getValue();
            try {
                lock.lock();
                LOGGER.debug("Cleaning cache: {} lock, Locked", entry.getKey());
                datasetIdLocksMap.remove(entry.getKey());
                LOGGER.debug("Dataset id maps cache cleaned");
            } finally {
                lock.unlock();
                LOGGER.debug("Cleaning cache: {} lock, Unlocked", entry.getKey());
            }
        }
    }

    private static final class DatasetProblemPatternAnalysisView<T> {

        @JsonProperty
        private final String datasetId;
        @JsonProperty
        private final T executionStep;
        @JsonProperty
        private final String executionTimestamp;
        @JsonProperty
        private final List<ProblemPattern> problemPatternList;

        private DatasetProblemPatternAnalysisView(DatasetProblemPatternAnalysis<T> datasetProblemPatternAnalysis) {
            this.datasetId = datasetProblemPatternAnalysis.getDatasetId();
            this.executionStep = datasetProblemPatternAnalysis.getExecutionStep();
            this.executionTimestamp = datasetProblemPatternAnalysis.getExecutionTimestamp() == null ? null :
                    datasetProblemPatternAnalysis.getExecutionTimestamp().toString();
            this.problemPatternList = getSortedProblemPatternList(datasetProblemPatternAnalysis);
        }

        private static <T> DatasetProblemPatternAnalysisView<T> getEmptyDatasetProblemPatternAnalysisView() {
            return new DatasetProblemPatternAnalysisView<>(new DatasetProblemPatternAnalysis<>("0", null, null,
                    new ArrayList<>()));
        }

        @NotNull
        private List<ProblemPattern> getSortedProblemPatternList(DatasetProblemPatternAnalysis<T> datasetProblemPatternAnalysis) {
            return datasetProblemPatternAnalysis
                    .getProblemPatternList()
                    .stream()
                    .map(DatasetProblemPatternAnalysisFilter::cleanMessageReportForP7TitleIsEnough)
                    .map(DatasetProblemPatternAnalysisFilter::sortRecordAnalysisByRecordId)
                    .sorted(Comparator.comparing(problemPattern -> problemPattern.getProblemPatternDescription().getProblemPatternId()))
                    .collect(Collectors.toList());
        }
    }

    private static final class DatasetProblemPatternAnalysisFilter {
        /**
         * Sort record analysis by record id problem pattern.
         *
         * @param problemPattern the problem pattern
         * @return the problem pattern
         */
        public static ProblemPattern sortRecordAnalysisByRecordId(ProblemPattern problemPattern) {
            return new ProblemPattern(problemPattern.getProblemPatternDescription(),
                    problemPattern.getRecordOccurrences(),
                    problemPattern.getRecordAnalysisList()
                            .stream()
                            .sorted(Comparator.comparing(RecordAnalysis::getRecordId))
                            .collect(Collectors.toList()));
        }

        /**
         * Clean message report for p 7 title is enough problem pattern.
         *
         * @param problemPattern the problem pattern
         * @return the problem pattern
         */
        public static ProblemPattern cleanMessageReportForP7TitleIsEnough(ProblemPattern problemPattern) {
            if (problemPattern.getProblemPatternDescription().getProblemPatternId().equals(ProblemPatternId.P7)) {
                return new ProblemPattern(problemPattern.getProblemPatternDescription(),
                        problemPattern.getRecordOccurrences(),
                        problemPattern.getRecordAnalysisList()
                                .stream()
                                .map(recordAnalysis -> new RecordAnalysis(recordAnalysis.getRecordId(),
                                        recordAnalysis.getProblemOccurrenceList()
                                                .stream()
                                                .map(problemOccurrence -> new ProblemOccurrence("", problemOccurrence.getAffectedRecordIds()))
                                                .collect(Collectors.toList())
                                ))
                                .collect(Collectors.toList()));
            } else {
                return problemPattern;
            }
        }
    }
}
