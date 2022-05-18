package eu.europeana.metis.sandbox.service.problempatterns;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElseGet;

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
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
   * @param executionPointRepository the execution point repository
   * @param datasetProblemPatternRepository the dataset problem pattern repository
   * @param datasetProblemPatternJdbcRepository the dataset problem pattern jdbc repository
   * @param recordProblemPatternRepository the record problem pattern repository
   * @param recordProblemPatternOccurrenceRepository the record problem pattern occurrence repository
   * @param recordTitleRepository the record title repository
   * @param recordTitleJdbcRepository the record title jdbc repository
   * @param maxRecordsPerPattern the max records per pattern allowed
   * @param maxProblemPatternOccurrences the max problem pattern occurrences per record allowed
   */
  public PatternAnalysisServiceImpl(ExecutionPointRepository executionPointRepository,
      DatasetProblemPatternRepository datasetProblemPatternRepository,
      DatasetProblemPatternJdbcRepository datasetProblemPatternJdbcRepository,
      RecordProblemPatternRepository recordProblemPatternRepository,
      RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository,
      RecordTitleRepository recordTitleRepository,
      RecordTitleJdbcRepository recordTitleJdbcRepository,
      @Value("${sandbox.problempatterns.max-records-per-pattern:10}") int maxRecordsPerPattern,
      @Value("${sandbox.problempatterns.max-problem-pattern-occurrences:10}") int maxProblemPatternOccurrences) {
    this.executionPointRepository = executionPointRepository;
    this.datasetProblemPatternRepository = datasetProblemPatternRepository;
    this.datasetProblemPatternJdbcRepository = datasetProblemPatternJdbcRepository;
    this.recordProblemPatternRepository = recordProblemPatternRepository;
    this.recordProblemPatternOccurrenceRepository = recordProblemPatternOccurrenceRepository;
    this.recordTitleRepository = recordTitleRepository;
    this.recordTitleJdbcRepository = recordTitleJdbcRepository;
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
        final Integer recordOccurrences = datasetProblemPatternJdbcRepository.upsertUpdateCounter(
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
    //Remove redundant titles
    recordTitleJdbcRepository.deleteRedundantRecordTitles(executionPoint.getExecutionPointId());

    //Insert global problem patterns to db
    final List<RecordTitle> duplicateRecordTitles = recordTitleRepository.findAllByExecutionPoint(executionPoint);
    final Map<String, List<RecordTitle>> groupedByTitle = duplicateRecordTitles.stream().collect(
        Collectors.groupingBy(recordTitle -> recordTitle.getRecordTitleCompositeKey().getTitle()));
    final int recordOccurrences = Math.toIntExact(duplicateRecordTitles.stream().map(RecordTitle::getRecordTitleCompositeKey)
                                                                       .map(RecordTitleCompositeKey::getRecordId).distinct()
                                                                       .count());
    //Update counter. Idempotent for 0 occurrences
    datasetProblemPatternJdbcRepository.upsertUpdateCounter(executionPoint.getExecutionPointId(), ProblemPatternId.P1.name(),
        recordOccurrences);

    //Titles are limited as well
    int titleOccurrences = 0;
    for (Entry<String, List<RecordTitle>> entry : groupedByTitle.entrySet()) {

      if (titleOccurrences <= maxRecordsPerPattern) {
        //For each title we will insert up to a max record problem pattern
        entry.getValue().stream().limit(maxRecordsPerPattern).forEach(recordTitle -> {

          final RecordProblemPattern recordProblemPattern = new RecordProblemPattern();
          recordProblemPattern.setPatternId(ProblemPatternId.P1.name());
          recordProblemPattern.setRecordId(recordTitle.getRecordTitleCompositeKey().getRecordId());
          recordProblemPattern.setExecutionPoint(executionPoint);
          final RecordProblemPattern savedRecordProblemPattern = this.recordProblemPatternRepository.save(recordProblemPattern);

          final RecordProblemPatternOccurrence recordProblemPatternOccurrence = new RecordProblemPatternOccurrence();
          recordProblemPatternOccurrence.setRecordProblemPattern(savedRecordProblemPattern);
          // TODO: 17/05/2022 Perhaps move this to the analyzer from metis-framework and call a method to create this.
          // This way the message report will be centralized
          recordProblemPatternOccurrence.setMessageReport(
              String.format("Systematic use of the same title: %s", recordTitle));
          this.recordProblemPatternOccurrenceRepository.save(recordProblemPatternOccurrence);
        });
      }
      titleOccurrences++;
    }

    //Remove remaining titles to avoid re-computation of titles
    recordTitleRepository.deleteByExecutionPoint(executionPoint);
  }

  private ArrayList<ProblemPattern> constructProblemPatterns(ExecutionPoint executionPoint) {
    final ArrayList<ProblemPattern> problemPatterns = new ArrayList<>();
    for (DatasetProblemPattern datasetProblemPattern : executionPoint.getDatasetProblemPatterns()) {

      final ArrayList<RecordAnalysis> recordAnalyses = getRecordAnalysesForPatternId(executionPoint,
          datasetProblemPattern.getDatasetProblemPatternCompositeKey().getPatternId());
      if (CollectionUtils.isNotEmpty(recordAnalyses)) {
        problemPatterns.add(new ProblemPattern(
            ProblemPatternDescription.fromName(datasetProblemPattern.getDatasetProblemPatternCompositeKey().getPatternId()),
            datasetProblemPattern.getRecordOccurrences(), recordAnalyses));
      }
    }
    return problemPatterns;
  }

  private ArrayList<RecordAnalysis> getRecordAnalysesForPatternId(ExecutionPoint executionPoint,
      String datasetProblemPatternId) {
    final ArrayList<RecordAnalysis> recordAnalyses = new ArrayList<>();

    final Map<String, List<String>> messageReportRecordIdsMap = new HashMap<>();
    //Only select the specific one we need
    executionPoint.getRecordProblemPatterns().stream()
                  .filter(recordProblemPattern -> datasetProblemPatternId.equals(recordProblemPattern.getPatternId()))
                  .forEach(recordProblemPattern -> {
                    final ArrayList<ProblemOccurrence> problemOccurrences = new ArrayList<>();
                    for (RecordProblemPatternOccurrence recordProblemPatternOccurrence : recordProblemPattern.getRecordProblemPatternOccurences()) {
                      //For P1 we need to collect the correct values first
                      if (datasetProblemPatternId.equals(ProblemPatternId.P1.name())) {
                        messageReportRecordIdsMap.computeIfAbsent(recordProblemPatternOccurrence.getMessageReport(),
                                                     k -> new ArrayList<>())
                                                 .add(recordProblemPattern.getRecordId());
                      } else {
                        problemOccurrences.add(new ProblemOccurrence(recordProblemPatternOccurrence.getMessageReport()));
                      }
                    }
                    recordAnalyses.add(new RecordAnalysis(recordProblemPattern.getRecordId(), problemOccurrences));
                  });

    if (datasetProblemPatternId.equals(ProblemPatternId.P1.name())) {
      //Clear first, this is a special pattern
      recordAnalyses.clear();
      for (Entry<String, List<String>> entry : messageReportRecordIdsMap.entrySet()) {
        //Add only the first recordId(there should always be at least one) and inside the problemOccurrence should contain a list of record ids
        recordAnalyses.add(
            new RecordAnalysis(entry.getValue().get(0), List.of(new ProblemOccurrence(entry.getKey(), entry.getValue()))));
      }
    }

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
    List<ProblemPattern> result = problemPatternAnalyzer.analyzeRecord(rdfRecord).getProblemPatterns();
    return requireNonNullElseGet(result, ArrayList::new);
  }
}
