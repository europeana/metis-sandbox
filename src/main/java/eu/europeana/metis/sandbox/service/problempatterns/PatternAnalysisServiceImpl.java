package eu.europeana.metis.sandbox.service.problempatterns;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPattern;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPatternId;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordProblemPattern;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordProblemPatternOccurrence;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternOccurrenceRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternRepository;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.metis.schema.jibx.RDF;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.ProblemPatternAnalyzer;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.patternanalysis.view.DatasetProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemOccurrence;
import eu.europeana.patternanalysis.view.ProblemPattern;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import eu.europeana.patternanalysis.view.RecordAnalysis;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Problem pattern analysis service implementation.
 */
@Service
public class PatternAnalysisServiceImpl implements PatternAnalysisService<Step> {

  private final ExecutionPointRepository executionPointRepository;
  private final DatasetProblemPatternRepository datasetProblemPatternRepository;
  private final RecordProblemPatternRepository recordProblemPatternRepository;
  private final RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository;
  private final ProblemPatternAnalyzer problemPatternAnalyzer = new ProblemPatternAnalyzer();
  private final int maxProblemPatternOccurences;
  private final int maxRecordsPerPattern;

  /**
   * Constructor with required parameters.
   *
   * @param executionPointRepository the execution point repository
   * @param datasetProblemPatternRepository the dataset problem pattern repository
   * @param recordProblemPatternRepository the record problem pattern repository
   * @param recordProblemPatternOccurrenceRepository the record problem pattern occurrence repository
   * @param maxRecordsPerPattern the max records per pattern allowed
   * @param maxProblemPatternOccurrences the max problem pattern occurrences per record allowed
   */
  public PatternAnalysisServiceImpl(ExecutionPointRepository executionPointRepository,
      DatasetProblemPatternRepository datasetProblemPatternRepository,
      RecordProblemPatternRepository recordProblemPatternRepository,
      RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository,
      @Value("${sandbox.problempatterns.max-records-per-pattern:10}") int maxRecordsPerPattern,
      @Value("${sandbox.problempatterns.max-problem-pattern-occurrences:10}") int maxProblemPatternOccurrences) {
    this.executionPointRepository = executionPointRepository;
    this.datasetProblemPatternRepository = datasetProblemPatternRepository;
    this.recordProblemPatternRepository = recordProblemPatternRepository;
    this.recordProblemPatternOccurrenceRepository = recordProblemPatternOccurrenceRepository;
    this.maxRecordsPerPattern = maxRecordsPerPattern;
    this.maxProblemPatternOccurences = maxProblemPatternOccurrences;
  }

  private ExecutionPoint initializePatternAnalysisExecution(String datasetId, Step executionStep,
      LocalDateTime executionTimestamp) {
    // TODO: 14/04/2022 This step could maybe be optimized by keeping an in memory cache of the execution
    final ExecutionPoint dbExecutionPoint = this.executionPointRepository.findByDatasetIdAndExecutionStepAndExecutionTimestamp(
        datasetId, executionStep.name(), executionTimestamp);
    final ExecutionPoint savedExecutionPoint;
    if (Objects.isNull(dbExecutionPoint)) {
      final ExecutionPoint executionPoint = new ExecutionPoint();
      executionPoint.setExecutionTimestamp(executionTimestamp);
      executionPoint.setExecutionStep(executionStep.name());
      executionPoint.setDatasetId(datasetId);
      savedExecutionPoint = this.executionPointRepository.save(executionPoint);

      //Initialize with zero all patterns
      final List<DatasetProblemPattern> datasetProblemPatterns = Arrays.stream(ProblemPatternDescription.values()).map(Enum::name)
                                                                       .map(patternId -> new DatasetProblemPattern(
                                                                           new DatasetProblemPatternId(
                                                                               savedExecutionPoint.getExecutionPointId(),
                                                                               patternId), savedExecutionPoint, 0))
                                                                       .collect(Collectors.toList());
      datasetProblemPatternRepository.saveAll(datasetProblemPatterns);
    } else {
      savedExecutionPoint = dbExecutionPoint;
    }

    return savedExecutionPoint;
  }

  private void insertPatternAnalysis(ExecutionPoint executionPoint, final List<ProblemPattern> problemPatterns) {
    for (ProblemPattern problemPattern : problemPatterns) {
      for (RecordAnalysis recordAnalysis : problemPattern.getRecordAnalysisList()) {
        final DatasetProblemPatternId datasetProblemPatternId = new DatasetProblemPatternId(executionPoint.getExecutionPointId(),
            problemPattern.getProblemPatternDescription().getProblemPatternId().name());
        this.datasetProblemPatternRepository.updateCounter(datasetProblemPatternId);
        final Integer recordOccurences = this.datasetProblemPatternRepository.findByDatasetProblemPatternId(
            datasetProblemPatternId).getRecordOccurrences();

        if (recordOccurences <= maxRecordsPerPattern) {
          final RecordProblemPattern recordProblemPattern = new RecordProblemPattern();
          recordProblemPattern.setPatternId(problemPattern.getProblemPatternDescription().getProblemPatternId().name());
          recordProblemPattern.setRecordId(recordAnalysis.getRecordId());
          recordProblemPattern.setExecutionPoint(executionPoint);
          final RecordProblemPattern savedRecordProblemPattern = this.recordProblemPatternRepository.save(recordProblemPattern);

          recordAnalysis.getProblemOccurrenceList().stream().limit(maxProblemPatternOccurences).forEach(problemOccurence -> {
            final RecordProblemPatternOccurrence recordProblemPatternOccurrence = new RecordProblemPatternOccurrence();
            recordProblemPatternOccurrence.setRecordProblemPattern(savedRecordProblemPattern);
            recordProblemPatternOccurrence.setMessageReport(problemOccurence.getMessageReport());
            this.recordProblemPatternOccurrenceRepository.save(recordProblemPatternOccurrence);
          });
        }
      }
    }
  }

  @Override
  @Transactional
  public void generateRecordPatternAnalysis(String datasetId, Step executionStep, LocalDateTime executionTimestamp,
      RDF rdfRecord) {
    final List<ProblemPattern> problemPatterns = problemPatternAnalyzer.analyzeRecord(rdfRecord);
    final ExecutionPoint executionPoint = initializePatternAnalysisExecution(datasetId, executionStep, executionTimestamp);
    insertPatternAnalysis(executionPoint, problemPatterns);
  }

  @Override
  @Transactional
  public void generateRecordPatternAnalysis(String datasetId, Step executionStep, LocalDateTime executionTimestamp,
      String rdfRecord) throws PatternAnalysisException {
    try {
      final List<ProblemPattern> problemPatterns = problemPatternAnalyzer.analyzeRecord(rdfRecord);
      final ExecutionPoint executionPoint = initializePatternAnalysisExecution(datasetId, executionStep, executionTimestamp);
      insertPatternAnalysis(executionPoint, problemPatterns);
    } catch (SerializationException e) {
      throw new PatternAnalysisException("Error during record analysis", e);
    }
  }

  @Override
  @Transactional
  public void finalizeDatasetPatternAnalysis(String datasetId, Step executionStep, LocalDateTime executionTimestamp) {
    //This is currently meant to be implemented for the P1 with the titles which will come later on.
  }

  @Override
  @Transactional
  public Optional<DatasetProblemPatternAnalysis<Step>> getDatasetPatternAnalysis(String datasetId, Step executionStep,
      LocalDateTime executionTimestamp) {
    final ExecutionPoint executionPoint = executionPointRepository.findByDatasetIdAndExecutionStepAndExecutionTimestamp(
        datasetId, executionStep.name(), executionTimestamp);
    if (nonNull(executionPoint)) {
      final ArrayList<ProblemPattern> problemPatterns = constructProblemPatterns(executionPoint);
      return Optional.of(new DatasetProblemPatternAnalysis<>(datasetId, executionStep, executionTimestamp, problemPatterns));
    }
    return Optional.empty();
  }

  private ArrayList<ProblemPattern> constructProblemPatterns(ExecutionPoint executionPoint) {
    return constructProblemPatterns(executionPoint, x -> true);
  }

  private ArrayList<ProblemPattern> constructProblemPatterns(ExecutionPoint executionPoint,
      Predicate<RecordProblemPattern> recordProblemPatternPredicate) {
    final ArrayList<ProblemPattern> problemPatterns = new ArrayList<>();
    for (DatasetProblemPattern datasetProblemPattern : executionPoint.getDatasetProblemPatterns()) {

      final ArrayList<RecordAnalysis> recordAnalyses = new ArrayList<>();
      executionPoint.getRecordProblemPatterns().stream()
                    .filter(recordProblemPatternPredicate)
                    .forEach(recordProblemPattern -> {
                      final ArrayList<ProblemOccurrence> problemOccurences = new ArrayList<>();
                      for (RecordProblemPatternOccurrence recordProblemPatternOccurrence : recordProblemPattern.getRecordProblemPatternOccurences()) {
                        problemOccurences.add(new ProblemOccurrence(recordProblemPatternOccurrence.getMessageReport()));
                      }
                      recordAnalyses.add(new RecordAnalysis(recordProblemPattern.getRecordId(), problemOccurences));
                    });
      problemPatterns.add(new ProblemPattern(
          ProblemPatternDescription.fromName(datasetProblemPattern.getDatasetProblemPatternId().getPatternId()),
          datasetProblemPattern.getRecordOccurrences(), recordAnalyses));
    }
    return problemPatterns;
  }

  @Override
  @Transactional
  public List<ProblemPattern> getRecordPatternAnalysis(String datasetId, Step executionStep, LocalDateTime executionTimestamp,
      RDF rdfRecord) {
    List<ProblemPattern> problemPatterns = new ArrayList<>();
    final ExecutionPoint executionPoint = executionPointRepository.findByDatasetIdAndExecutionStepAndExecutionTimestamp(
        datasetId, executionStep.name(), executionTimestamp);
    //Check if execution exists and there is data for the specific record id.
    if (nonNull(executionPoint)) {
      //construct only the ones that we are interested in
      problemPatterns = constructProblemPatterns(executionPoint,
          recordProblemPattern -> recordProblemPattern.getRecordId().equals(rdfRecord.getProvidedCHOList().get(0).getAbout()));
    }
    if (isEmpty(problemPatterns)) {
      problemPatterns = problemPatternAnalyzer.analyzeRecord(rdfRecord);
    }

    return problemPatterns;
  }
}
