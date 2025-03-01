package eu.europeana.metis.sandbox.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

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
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

        // Get the execution point. If it does not exist, we are done.
        final ExecutionPoint executionPoint = executionPointService
            .getExecutionPoint(datasetId, Step.VALIDATE_INTERNAL.toString()).orElse(null);
        if (executionPoint == null) {
            return new ResponseEntity<>(DatasetProblemPatternAnalysisView.getEmptyAnalysis(datasetId,
                ProblemPatternAnalysisStatus.PENDING), HttpStatus.NOT_FOUND);
        }

        // Finalize the problem pattern analysis if we can (i.e. if the dataset finished processing).
        final ProblemPatternAnalysisStatus status = finalizeDatasetPatternAnalysis(datasetId, executionPoint);

        // Now get the pattern analysis.
        final DatasetProblemPatternAnalysisView<Step> analysisView = patternAnalysisService.getDatasetPatternAnalysis(
                datasetId, Step.VALIDATE_INTERNAL, executionPoint.getExecutionTimestamp())
            .map(analysis -> new DatasetProblemPatternAnalysisView<>(analysis, status))
            .orElseGet(() -> {
                LOGGER.error("Result not expected to be empty when there is a non-null execution point.");
                return DatasetProblemPatternAnalysisView.getEmptyAnalysis(datasetId,
                    ProblemPatternAnalysisStatus.ERROR);
            });

        // Wrap and return.
        final HttpStatus httpStatus = analysisView.analysisStatus == ProblemPatternAnalysisStatus.ERROR
            ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.OK;
        return new ResponseEntity<>(analysisView, httpStatus);
    }

    private ProblemPatternAnalysisStatus finalizeDatasetPatternAnalysis(String datasetId,
            ExecutionPoint datasetExecutionPoint) {
        if (datasetReportService.getReport(datasetId).getStatus() == Status.COMPLETED) {
            final Lock lock = datasetIdLocksMap.computeIfAbsent(datasetId, s -> lockRegistry.obtain("finalizePatternAnalysis_" + datasetId));
            try {
                lock.lock();
                LOGGER.debug("Finalize analysis: {} lock, Locked", datasetId);
                patternAnalysisService.finalizeDatasetPatternAnalysis(datasetExecutionPoint);
                return ProblemPatternAnalysisStatus.FINALIZED;
            } catch (PatternAnalysisException e) {
                LOGGER.error("Something went wrong during finalizing pattern analysis", e);
                return ProblemPatternAnalysisStatus.ERROR;
            } finally {
                lock.unlock();
                LOGGER.debug("Finalize analysis: {} lock, Unlocked", datasetId);
            }
        }
        // This method is only executed if we have an execution point, i.e. if processing is underway.
        return ProblemPatternAnalysisStatus.IN_PROGRESS;
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
        @Parameter(description = "The record content as a file", required = true) @RequestParam("recordId") String recordId)
            throws SerializationException {
        RecordLogEntity recordLog = recordLogService.getRecordLogEntity(recordId, datasetId, Step.VALIDATE_INTERNAL);
        return recordLog == null ? new ResponseEntity<>(HttpStatus.NOT_FOUND) :
                new ResponseEntity<>(
                        patternAnalysisService.getRecordPatternAnalysis(rdfConversionUtils.convertStringToRdf(recordLog.getContent()))
                                .stream()
                                .map(DatasetProblemPatternAnalysisFilter::cleanMessageReportForP7TitleIsEnough)
                                .map(DatasetProblemPatternAnalysisFilter::sortRecordAnalysisByRecordId)
                                .sorted(Comparator.comparing(problemPattern -> problemPattern.getProblemPatternDescription().getProblemPatternId()))
                                .toList(),
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

    enum ProblemPatternAnalysisStatus {PENDING, IN_PROGRESS, FINALIZED, ERROR}

    private static final class DatasetProblemPatternAnalysisView<T> {

        @JsonProperty
        private final String datasetId;
        @JsonProperty
        private final T executionStep;
        @JsonProperty
        private final String executionTimestamp;
        @JsonProperty
        private final List<ProblemPattern> problemPatternList;
        @JsonProperty
        private final ProblemPatternAnalysisStatus analysisStatus;

        private DatasetProblemPatternAnalysisView(DatasetProblemPatternAnalysis<T> datasetProblemPatternAnalysis,
                ProblemPatternAnalysisStatus status) {
            this.datasetId = datasetProblemPatternAnalysis.getDatasetId();
            this.executionStep = datasetProblemPatternAnalysis.getExecutionStep();
            this.executionTimestamp = datasetProblemPatternAnalysis.getExecutionTimestamp() == null ? null :
                    datasetProblemPatternAnalysis.getExecutionTimestamp().toString();
            this.problemPatternList = getSortedProblemPatternList(datasetProblemPatternAnalysis);
            this.analysisStatus = status;
        }

        private static <T> DatasetProblemPatternAnalysisView<T> getEmptyAnalysis(String datasetId,
            ProblemPatternAnalysisStatus status) {
            return new DatasetProblemPatternAnalysisView<>(new DatasetProblemPatternAnalysis<>(
                datasetId, null, null, new ArrayList<>()), status);
        }

        @NotNull
        private List<ProblemPattern> getSortedProblemPatternList(DatasetProblemPatternAnalysis<T> datasetProblemPatternAnalysis) {
            return datasetProblemPatternAnalysis
                    .getProblemPatternList()
                    .stream()
                    .map(DatasetProblemPatternAnalysisFilter::cleanMessageReportForP7TitleIsEnough)
                    .map(DatasetProblemPatternAnalysisFilter::sortRecordAnalysisByRecordId)
                    .sorted(Comparator.comparing(problemPattern -> problemPattern.getProblemPatternDescription().getProblemPatternId()))
                    .toList();
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
                            .toList());
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
                                                .toList()
                                ))
                                .toList());
            } else {
                return problemPattern;
            }
        }
    }
}
