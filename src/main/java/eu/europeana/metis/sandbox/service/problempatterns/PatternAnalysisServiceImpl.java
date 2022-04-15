package eu.europeana.metis.sandbox.service.problempatterns;

import static java.util.Objects.nonNull;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPattern;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPatternId;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordProblemPattern;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordProblemPatternOccurence;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternOccurenceRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternRepository;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.metis.schema.jibx.RDF;
import eu.europeana.patternanalysis.PatternAnalysisException;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.ProblemPatternAnalyzer;
import eu.europeana.patternanalysis.view.DatasetProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemOccurence;
import eu.europeana.patternanalysis.view.ProblemPattern;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import eu.europeana.patternanalysis.view.RecordAnalysis;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class PatternAnalysisServiceImpl implements PatternAnalysisService<Step> {

  private final ExecutionPointRepository executionPointRepository;
  private final DatasetProblemPatternRepository datasetProblemPatternRepository;
  private final RecordProblemPatternRepository recordProblemPatternRepository;
  private final RecordProblemPatternOccurenceRepository recordProblemPatternOccurenceRepository;
  private final ProblemPatternAnalyzer problemPatternAnalyzer = new ProblemPatternAnalyzer();

  public PatternAnalysisServiceImpl(ExecutionPointRepository executionPointRepository,
      DatasetProblemPatternRepository datasetProblemPatternRepository,
      RecordProblemPatternRepository recordProblemPatternRepository,
      RecordProblemPatternOccurenceRepository recordProblemPatternOccurenceRepository) {
    this.datasetProblemPatternRepository = datasetProblemPatternRepository;
    this.recordProblemPatternRepository = recordProblemPatternRepository;
    this.recordProblemPatternOccurenceRepository = recordProblemPatternOccurenceRepository;
    this.executionPointRepository = executionPointRepository;
  }

  private ExecutionPoint initializePatternAnalysisExecution(String datasetId, Step executionStep, LocalDateTime executionTimestamp) {
    final ExecutionPoint executionPoint = new ExecutionPoint();
    executionPoint.setExecutionTimestamp(executionTimestamp);
    executionPoint.setExecutionStep(executionStep.name());
    executionPoint.setDatasetId(datasetId);
    return this.executionPointRepository.save(executionPoint);
  }

  private void insertPatternAnalysis(ExecutionPoint executionPoint, final List<ProblemPattern> problemPatterns) {
    for (ProblemPattern problemPattern : problemPatterns) {
      for (RecordAnalysis recordAnalysis : problemPattern.getRecordAnalysisList()) {
        // TODO: 14/04/2022 Check dataset problem pattern and if above a threshold we should only update the occurences on the dataset table and not insert more records
        // TODO: 14/04/2022 Convert to upsert to increase the counter
        final DatasetProblemPattern datasetProblemPattern = new DatasetProblemPattern();
        final DatasetProblemPatternId datasetProblemPatternId = new DatasetProblemPatternId();
        datasetProblemPatternId.setPatternId(problemPattern.getProblemPatternDescription().getProblemPatternId().name());
        datasetProblemPattern.setDatasetProblemPatternId(datasetProblemPatternId);
        datasetProblemPattern.setExecutionPoint(executionPoint);
        datasetProblemPattern.setRecordOccurences(1);
        this.datasetProblemPatternRepository.save(datasetProblemPattern);

        // TODO: 14/04/2022 Update this according the value from above
        if (true) {
          final RecordProblemPattern recordProblemPattern = new RecordProblemPattern();
          recordProblemPattern.setPatternId(problemPattern.getProblemPatternDescription().getProblemPatternId().name());
          recordProblemPattern.setRecordId(recordAnalysis.getRecordId());
          recordProblemPattern.setExecutionPoint(executionPoint);
          final RecordProblemPattern savedRecordProblemPattern = this.recordProblemPatternRepository.save(recordProblemPattern);

          for (ProblemOccurence problemOccurence : recordAnalysis.getProblemOccurenceList()) {
            final RecordProblemPatternOccurence recordProblemPatternOccurence = new RecordProblemPatternOccurence();
            recordProblemPatternOccurence.setRecordProblemPattern(savedRecordProblemPattern);
            recordProblemPatternOccurence.setMessageReport(problemOccurence.getMessageReport());
            this.recordProblemPatternOccurenceRepository.save(recordProblemPatternOccurence);
          }
        }
      }
    }
  }

  @Override
  public void generateRecordPatternAnalysis(String datasetId, Step executionStep, LocalDateTime executionTimestamp,
      RDF rdfRecord) {
    final List<ProblemPattern> problemPatterns = problemPatternAnalyzer.analyzeRecord(rdfRecord);
    // TODO: 14/04/2022 This step could maybe be optimized by keeping an in memory cache of the execution
    final ExecutionPoint executionPoint = initializePatternAnalysisExecution(datasetId, executionStep, executionTimestamp);
    insertPatternAnalysis(executionPoint, problemPatterns);
  }

  @Override
  public void generateRecordPatternAnalysis(String datasetId, Step executionStep, LocalDateTime executionTimestamp,
      String rdfRecord) throws PatternAnalysisException {
    try {
      final List<ProblemPattern> problemPatterns = problemPatternAnalyzer.analyzeRecord(rdfRecord);
      // TODO: 14/04/2022 This step could maybe be optimized by keeping an in memory cache of the execution
      final ExecutionPoint executionPoint = initializePatternAnalysisExecution(datasetId, executionStep, executionTimestamp);
      insertPatternAnalysis(executionPoint, problemPatterns);
    } catch (SerializationException e) {
      throw new PatternAnalysisException("Error during record analysis", e);
    }
  }

  @Override
  public void finalizeDatasetPatternAnalysis(String datasetId, Step executionStep, LocalDateTime executionTimestamp) {

  }

  @Override
  public Optional<DatasetProblemPatternAnalysis<Step>> getDatasetPatternAnalysis(String datasetId, Step executionStep,
      LocalDateTime executionTimestamp) {
    final ExecutionPoint executionPoint = executionPointRepository.findByDatasetIdAndExecutionStepAndExecutionTimestamp(
        datasetId, executionStep.name(), executionTimestamp);
    if (nonNull(executionPoint)) {

      final ArrayList<ProblemPattern> problemPatterns = new ArrayList<>();
      for (DatasetProblemPattern datasetProblemPattern : executionPoint.getDatasetProblemPatterns()) {

        final ArrayList<RecordAnalysis> recordAnalyses = new ArrayList<>();
        for (RecordProblemPattern recordProblemPattern : executionPoint.getRecordProblemPatterns()) {
          final ArrayList<ProblemOccurence> problemOccurences = new ArrayList<>();
          for (RecordProblemPatternOccurence recordProblemPatternOccurence : recordProblemPattern.getRecordProblemPatternOccurences()) {
            problemOccurences.add(new ProblemOccurence(recordProblemPatternOccurence.getMessageReport()));
          }
          recordAnalyses.add(new RecordAnalysis(recordProblemPattern.getRecordId(), problemOccurences));
        }
        problemPatterns.add(new ProblemPattern(
            ProblemPatternDescription.fromName(datasetProblemPattern.getDatasetProblemPatternId().getPatternId()),
            datasetProblemPattern.getRecordOccurences(), recordAnalyses));
      }
      return Optional.of(new DatasetProblemPatternAnalysis<Step>(datasetId, executionStep, executionTimestamp, problemPatterns));
    }
    return Optional.empty();
  }

  @Override
  public List<ProblemPattern> getRecordPatternAnalysis(String datasetId, Step executionStep, LocalDateTime executionTimestamp,
      RDF rdfRecord) {
    return null;
  }
}
