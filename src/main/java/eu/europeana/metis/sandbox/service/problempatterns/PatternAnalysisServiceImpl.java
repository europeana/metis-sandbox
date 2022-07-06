package eu.europeana.metis.sandbox.service.problempatterns;

import static java.util.Objects.nonNull;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPattern;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPatternCompositeKey;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordProblemPattern;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordProblemPatternOccurrence;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitle;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitleCompositeKey;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternJdbcRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternOccurrenceRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordTitleJdbcRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordTitleRepository;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.metis.schema.jibx.RDF;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.ProblemPatternAnalyzer;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.patternanalysis.view.DatasetProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemOccurrence;
import eu.europeana.patternanalysis.view.ProblemPattern;
import eu.europeana.patternanalysis.view.ProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import eu.europeana.patternanalysis.view.ProblemPatternDescription.ProblemPatternId;
import eu.europeana.patternanalysis.view.RecordAnalysis;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Problem pattern analysis service implementation.
 */
@Service
public class PatternAnalysisServiceImpl implements PatternAnalysisService<Step, ExecutionPoint> {

  private static final int DEFAULT_MAX_CHARACTERS_TITLE_LENGTH = 255;

  private final ExecutionPointRepository executionPointRepository;
  private final DatasetProblemPatternRepository datasetProblemPatternRepository;
  private final DatasetProblemPatternJdbcRepository datasetProblemPatternJdbcRepository;
  private final RecordProblemPatternRepository recordProblemPatternRepository;
  private final RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository;
  private final RecordTitleRepository recordTitleRepository;
  private final RecordTitleJdbcRepository recordTitleJdbcRepository;
  private final ProblemPatternAnalyzer problemPatternAnalyzer = new ProblemPatternAnalyzer();
  private final int maxProblemPatternOccurrences;
  private final int maxRecordsPerPattern;

  /**
   * Constructor with required parameters.
   *
   * @param problemPatternsRepositories the problem patterns repositories wrapper
   * @param maxRecordsPerPattern the max records per pattern allowed
   * @param maxProblemPatternOccurrences the max problem pattern occurrences per record allowed
   */
  public PatternAnalysisServiceImpl(ProblemPatternsRepositories problemPatternsRepositories,
      @Value("${sandbox.problempatterns.max-records-per-pattern:10}") int maxRecordsPerPattern,
      @Value("${sandbox.problempatterns.max-problem-pattern-occurrences:10}") int maxProblemPatternOccurrences) {
    this.executionPointRepository = problemPatternsRepositories.getExecutionPointRepository();
    this.datasetProblemPatternRepository = problemPatternsRepositories.getDatasetProblemPatternRepository();
    this.datasetProblemPatternJdbcRepository = problemPatternsRepositories.getDatasetProblemPatternJdbcRepository();
    this.recordProblemPatternRepository = problemPatternsRepositories.getRecordProblemPatternRepository();
    this.recordProblemPatternOccurrenceRepository = problemPatternsRepositories.getRecordProblemPatternOccurrenceRepository();
    this.recordTitleRepository = problemPatternsRepositories.getRecordTitleRepository();
    this.recordTitleJdbcRepository = problemPatternsRepositories.getRecordTitleJdbcRepository();
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
                                                                           new DatasetProblemPatternCompositeKey(
                                                                               savedExecutionPoint.getExecutionPointId(),
                                                                               patternId), savedExecutionPoint, 0))
                                                                       .collect(Collectors.toList());
      datasetProblemPatternRepository.saveAll(datasetProblemPatterns);
    } else {
      savedExecutionPoint = dbExecutionPoint;
    }

    return savedExecutionPoint;
  }

  private void insertPatternAnalysis(ExecutionPoint executionPoint, final ProblemPatternAnalysis problemPatternAnalysis) {
    for (ProblemPattern problemPattern : problemPatternAnalysis.getProblemPatterns()) {
      for (RecordAnalysis recordAnalysis : problemPattern.getRecordAnalysisList()) {
        final Integer recordOccurrences = datasetProblemPatternJdbcRepository.upsertCounter(
            executionPoint.getExecutionPointId(), problemPattern.getProblemPatternDescription().getProblemPatternId().name(), 1);

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

    final Set<RecordTitle> recordTitles = new HashSet<>();
    for (String title : problemPatternAnalysis.getTitles()) {
      final RecordTitleCompositeKey recordTitleCompositeKey = new RecordTitleCompositeKey(executionPoint.getExecutionPointId(),
          problemPatternAnalysis.getRdfAbout(), StringUtils.truncate(title, DEFAULT_MAX_CHARACTERS_TITLE_LENGTH));
      recordTitles.add(new RecordTitle(recordTitleCompositeKey, executionPoint));
    }
    this.recordTitleRepository.saveAll(recordTitles);
  }

  @Override
  @Transactional
  public void generateRecordPatternAnalysis(ExecutionPoint executionPoint,
      RDF rdfRecord) {
    final ProblemPatternAnalysis problemPatternAnalysis = problemPatternAnalyzer.analyzeRecord(rdfRecord);
    insertPatternAnalysis(executionPoint, problemPatternAnalysis);
  }

  @Override
  @Transactional
  public void generateRecordPatternAnalysis(ExecutionPoint executionPoint, String rdfRecord) throws PatternAnalysisException {
    try {
      final ProblemPatternAnalysis problemPatternAnalysis = problemPatternAnalyzer.analyzeRecord(rdfRecord);
      insertPatternAnalysis(executionPoint, problemPatternAnalysis);
    } catch (SerializationException e) {
      throw new PatternAnalysisException("Error during record analysis", e);
    }
  }

  @Override
  @Transactional
  public void finalizeDatasetPatternAnalysis(ExecutionPoint executionPoint) {
    recordTitleJdbcRepository.deleteRedundantRecordTitles(executionPoint.getExecutionPointId());

    //Insert global problem patterns to db
    final List<RecordTitle> duplicateRecordTitles = recordTitleRepository.findAllByExecutionPoint(executionPoint);
    final Map<String, List<RecordTitle>> groupByTitle = duplicateRecordTitles.stream().collect(
        Collectors.groupingBy(recordTitle -> recordTitle.getRecordTitleCompositeKey().getTitle()));

    final int totalRecordOccurrences = Math.toIntExact(duplicateRecordTitles.stream().map(RecordTitle::getRecordTitleCompositeKey)
                                                                            .map(RecordTitleCompositeKey::getRecordId).distinct()
                                                                            .count());
    //Update counter. Idempotent for 0 occurrences
    datasetProblemPatternJdbcRepository.upsertCounter(executionPoint.getExecutionPointId(), ProblemPatternId.P1.name(),
        totalRecordOccurrences);

    Map<String, RecordProblemPattern> recordIdsInserted = new HashMap<>();
    groupByTitle.entrySet().stream().limit(maxRecordsPerPattern).forEach(entry ->
        entry.getValue().stream().limit(maxRecordsPerPattern).forEach(recordTitle -> {
          RecordProblemPattern savedRecordProblemPattern = recordIdsInserted.get(
              recordTitle.getRecordTitleCompositeKey().getRecordId());
          if (savedRecordProblemPattern == null) {
            final RecordProblemPattern recordProblemPattern = new RecordProblemPattern();
            recordProblemPattern.setPatternId(ProblemPatternId.P1.name());
            recordProblemPattern.setRecordId(recordTitle.getRecordTitleCompositeKey().getRecordId());
            recordProblemPattern.setExecutionPoint(executionPoint);
            savedRecordProblemPattern = this.recordProblemPatternRepository.save(recordProblemPattern);
            recordIdsInserted.put(recordTitle.getRecordTitleCompositeKey().getRecordId(), savedRecordProblemPattern);
          }

          final RecordProblemPatternOccurrence recordProblemPatternOccurrence = new RecordProblemPatternOccurrence();
          recordProblemPatternOccurrence.setRecordProblemPattern(savedRecordProblemPattern);
          recordProblemPatternOccurrence.setMessageReport(problemPatternAnalyzer.abbreviateElement(entry.getKey()));
          this.recordProblemPatternOccurrenceRepository.save(recordProblemPatternOccurrence);
        })
    );

    //Remove remaining titles to avoid re-computation of titles
    recordTitleRepository.deleteByExecutionPoint(executionPoint);
  }

  private ArrayList<ProblemPattern> constructProblemPatterns(ExecutionPoint executionPoint) {
    final ArrayList<ProblemPattern> problemPatterns = new ArrayList<>();
    for (DatasetProblemPattern datasetProblemPattern : executionPoint.getDatasetProblemPatterns()) {

      final ArrayList<RecordAnalysis> recordAnalyses = getNonGlobalRecordAnalysesForPatternId(executionPoint,
          datasetProblemPattern.getDatasetProblemPatternCompositeKey().getPatternId());
      recordAnalyses.addAll(getGlobalRecordAnalysesForPatternId(executionPoint,
          datasetProblemPattern.getDatasetProblemPatternCompositeKey().getPatternId()));
      if (CollectionUtils.isNotEmpty(recordAnalyses)) {
        problemPatterns.add(new ProblemPattern(
            ProblemPatternDescription.fromName(datasetProblemPattern.getDatasetProblemPatternCompositeKey().getPatternId()),
            datasetProblemPattern.getRecordOccurrences(), recordAnalyses));
      }
    }
    return problemPatterns;
  }

  private ArrayList<RecordAnalysis> getNonGlobalRecordAnalysesForPatternId(ExecutionPoint executionPoint,
      String datasetProblemPatternId) {
    final ArrayList<RecordAnalysis> recordAnalyses = new ArrayList<>();

    //Only select the specific one we need
    getNonGlobalRecordProblemPatternStream(executionPoint, datasetProblemPatternId).forEach(recordProblemPattern -> {
      final ArrayList<ProblemOccurrence> problemOccurrences = new ArrayList<>();
      for (RecordProblemPatternOccurrence recordProblemPatternOccurrence : recordProblemPattern.getRecordProblemPatternOccurences()) {
        problemOccurrences.add(new ProblemOccurrence(recordProblemPatternOccurrence.getMessageReport()));
      }
      recordAnalyses.add(new RecordAnalysis(recordProblemPattern.getRecordId(), problemOccurrences));
    });
    return recordAnalyses;
  }

  private ArrayList<RecordAnalysis> getGlobalRecordAnalysesForPatternId(ExecutionPoint executionPoint,
      String datasetProblemPatternId) {
    final ArrayList<RecordAnalysis> recordAnalyses = new ArrayList<>();

    final Map<String, List<String>> messageReportRecordIdsMap = new HashMap<>();
    //Only select the specific one we need
    getGlobalRecordProblemPatternStream(executionPoint, datasetProblemPatternId).forEach(recordProblemPattern -> {
      for (RecordProblemPatternOccurrence recordProblemPatternOccurrence : recordProblemPattern.getRecordProblemPatternOccurences()) {
        //For P1 we need to collect the correct values first
        if (datasetProblemPatternId.equals(ProblemPatternId.P1.name())) {
          messageReportRecordIdsMap.computeIfAbsent(recordProblemPatternOccurrence.getMessageReport(),
                                       k -> new ArrayList<>())
                                   .add(recordProblemPattern.getRecordId());
        }
      }
    });

    if (datasetProblemPatternId.equals(ProblemPatternId.P1.name())) {
      for (Entry<String, List<String>> entry : messageReportRecordIdsMap.entrySet()) {
        //Add only the first recordId(there should always be at least one) and inside the problemOccurrence should contain a list of record ids
        recordAnalyses.add(
            new RecordAnalysis(entry.getValue().get(0), List.of(new ProblemOccurrence(entry.getKey(), entry.getValue()))));
      }
    }

    return recordAnalyses;
  }

  @NotNull
  private Stream<RecordProblemPattern> getNonGlobalRecordProblemPatternStream(ExecutionPoint executionPoint,
      String datasetProblemPatternId) {
    return executionPoint.getRecordProblemPatterns().stream()
                         .filter(recordProblemPattern -> datasetProblemPatternId.equals(recordProblemPattern.getPatternId()))
                         .filter(getRecordProblemPatternPredicate(datasetProblemPatternId,
                             ProblemPatternAnalyzer.nonGlobalProblemPatterns));
  }

  @NotNull
  private Stream<RecordProblemPattern> getGlobalRecordProblemPatternStream(ExecutionPoint executionPoint,
      String datasetProblemPatternId) {
    return executionPoint.getRecordProblemPatterns().stream()
                         .filter(recordProblemPattern -> datasetProblemPatternId.equals(recordProblemPattern.getPatternId()))
                         .filter(getRecordProblemPatternPredicate(datasetProblemPatternId,
                             ProblemPatternAnalyzer.globalProblemPatterns));
  }

  @NotNull
  private Predicate<RecordProblemPattern> getRecordProblemPatternPredicate(String datasetProblemPatternId,
      Set<ProblemPatternId> problemPatternIdSet) {
    return recordProblemPattern -> problemPatternIdSet.stream().map(ProblemPatternId::name)
                                                      .anyMatch(datasetProblemPatternId::equals);
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
    return problemPatternAnalyzer.analyzeRecord(rdfRecord).getProblemPatterns();
  }
}
