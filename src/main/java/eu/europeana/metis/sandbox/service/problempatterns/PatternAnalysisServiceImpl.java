package eu.europeana.metis.sandbox.service.problempatterns;

import static java.util.Objects.nonNull;

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
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Problem pattern analysis service implementation.
 */
@Service
public class PatternAnalysisServiceImpl implements PatternAnalysisService<Step, ExecutionPoint> {

  private final ExecutionPointRepository executionPointRepository;
  private final DatasetProblemPatternRepository datasetProblemPatternRepository;
  private final RecordProblemPatternRepository recordProblemPatternRepository;
  private final RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository;
  private final ProblemPatternAnalyzer problemPatternAnalyzer = new ProblemPatternAnalyzer();
  private final int maxProblemPatternOccurrences;
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
    this.maxProblemPatternOccurrences = maxProblemPatternOccurrences;
  }

  @Override
  @Transactional
  public ExecutionPoint initializePatternAnalysisExecution(String datasetId, Step executionStep,
      LocalDateTime executionTimestamp) {
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
      final List<DatasetProblemPattern> datasetProblemPatterns = Arrays.stream(ProblemPatternDescription.values())
                                                                       .map(Enum::name)
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
        // TODO: 03/05/2022 To make this thread safe, an upsert should be used instead of an update and get
        this.datasetProblemPatternRepository.updateCounter(datasetProblemPatternId);
        final Integer recordOccurrences = this.datasetProblemPatternRepository.findByDatasetProblemPatternId(
            datasetProblemPatternId).getRecordOccurrences();

        if (recordOccurrences <= maxRecordsPerPattern) {
          final RecordProblemPattern recordProblemPattern = new RecordProblemPattern();
          recordProblemPattern.setPatternId(problemPattern.getProblemPatternDescription().getProblemPatternId().name());
          recordProblemPattern.setRecordId(recordAnalysis.getRecordId());
          recordProblemPattern.setExecutionPoint(executionPoint);
          final RecordProblemPattern savedRecordProblemPattern = this.recordProblemPatternRepository.save(recordProblemPattern);

          recordAnalysis.getProblemOccurrenceList().stream().limit(maxProblemPatternOccurrences).forEach(problemOccurrence -> {
            final RecordProblemPatternOccurrence recordProblemPatternOccurrence = new RecordProblemPatternOccurrence();
            recordProblemPatternOccurrence.setRecordProblemPattern(savedRecordProblemPattern);
            recordProblemPatternOccurrence.setMessageReport(problemOccurrence.getMessageReport());
            this.recordProblemPatternOccurrenceRepository.save(recordProblemPatternOccurrence);
          });
        }
      }
    }
  }

  @Override
  @Transactional
  public void generateRecordPatternAnalysis(ExecutionPoint executionPoint,
      RDF rdfRecord) {
    final List<ProblemPattern> problemPatterns = problemPatternAnalyzer.analyzeRecord(rdfRecord);
    insertPatternAnalysis(executionPoint, problemPatterns);
  }

  @Override
  @Transactional
  public void generateRecordPatternAnalysis(ExecutionPoint executionPoint, String rdfRecord) throws PatternAnalysisException {
    try {
      final List<ProblemPattern> problemPatterns = problemPatternAnalyzer.analyzeRecord(rdfRecord);
      insertPatternAnalysis(executionPoint, problemPatterns);
    } catch (SerializationException e) {
      throw new PatternAnalysisException("Error during record analysis", e);
    }
  }

  @Override
  @Transactional
  public void finalizeDatasetPatternAnalysis(ExecutionPoint executionPoint) {
    //This is currently meant to be implemented for the P1 with the titles which will come later on.
  }

  private ArrayList<ProblemPattern> constructProblemPatterns(ExecutionPoint executionPoint) {
    final ArrayList<ProblemPattern> problemPatterns = new ArrayList<>();
    for (DatasetProblemPattern datasetProblemPattern : executionPoint.getDatasetProblemPatterns()) {

      final ArrayList<RecordAnalysis> recordAnalyses = getRecordAnalysesForPatternId(executionPoint,
          datasetProblemPattern.getDatasetProblemPatternId().getPatternId());
      if (CollectionUtils.isNotEmpty(recordAnalyses)) {
        problemPatterns.add(new ProblemPattern(
            ProblemPatternDescription.fromName(datasetProblemPattern.getDatasetProblemPatternId().getPatternId()),
            datasetProblemPattern.getRecordOccurrences(), recordAnalyses));
      }
    }
    return problemPatterns;
  }

  private ArrayList<RecordAnalysis> getRecordAnalysesForPatternId(ExecutionPoint executionPoint,
      String datasetProblemPatternId) {
    final ArrayList<RecordAnalysis> recordAnalyses = new ArrayList<>();
    executionPoint.getRecordProblemPatterns().forEach(recordProblemPattern -> {
      final ArrayList<ProblemOccurrence> problemOccurrences = new ArrayList<>();
      for (RecordProblemPatternOccurrence recordProblemPatternOccurrence : recordProblemPattern.getRecordProblemPatternOccurences()) {
        problemOccurrences.add(new ProblemOccurrence(recordProblemPatternOccurrence.getMessageReport()));
      }
      //Select only the relevant ones
      if (datasetProblemPatternId.equals(recordProblemPattern.getPatternId())) {
        recordAnalyses.add(new RecordAnalysis(recordProblemPattern.getRecordId(), problemOccurrences));
      }
    });
    return recordAnalyses;
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

  @Override
  @Transactional
  public List<ProblemPattern> getRecordPatternAnalysis(RDF rdfRecord) {
    List<ProblemPattern> result = problemPatternAnalyzer.analyzeRecord(rdfRecord);
    return result == null ? new ArrayList<>() : result;
  }
}
