package eu.europeana.metis.sandbox.service.problempatterns;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitle;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitleCompositeKey;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternOccurrenceRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordTitleRepository;
import eu.europeana.metis.schema.convert.RdfConversionUtils;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.metis.schema.jibx.RDF;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.patternanalysis.view.DatasetProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemPattern;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.Resource;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration(exclude = {EmbeddedMongoAutoConfiguration.class, MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class})
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "eu.europeana.metis.sandbox.repository.problempatterns")
@EntityScan(basePackages = "eu.europeana.metis.sandbox.entity.problempatterns")
@ComponentScan({"eu.europeana.metis.sandbox.service.problempatterns", "eu.europeana.metis.sandbox.repository.problempatterns"})
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=none",//We do not want hibernate creating the db
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL9Dialect"
})
@Sql("classpath:database/schema_problem_patterns.sql") //We want the sql script to create the db
class PatternAnalysisServiceImplIT extends PostgresContainerInitializerIT {

  final String rdfStringNoProblems = IOUtils.toString(
      new FileInputStream("src/test/resources/record.problempatterns/europeana_record_no_problem_patterns.xml"),
      StandardCharsets.UTF_8);
  final String rdfStringP2 = IOUtils.toString(
      new FileInputStream("src/test/resources/record.problempatterns/europeana_record_with_P2.xml"), StandardCharsets.UTF_8);
  final String rdfStringP2MultipleOccurrences = IOUtils.toString(
      new FileInputStream("src/test/resources/record.problempatterns/europeana_record_with_P2_multiple.xml"),
      StandardCharsets.UTF_8);
  final String rdfStringP6 = IOUtils.toString(
      new FileInputStream("src/test/resources/record.problempatterns/europeana_record_with_P6.xml"), StandardCharsets.UTF_8);
  final String rdfStringP12 = IOUtils.toString(
      new FileInputStream("src/test/resources/record.problempatterns/europeana_record_with_P12.xml"), StandardCharsets.UTF_8);

  final RDF rdfRecordNoProblems = new RdfConversionUtils().convertStringToRdf(rdfStringNoProblems);
  final RDF rdfRecordP2 = new RdfConversionUtils().convertStringToRdf(rdfStringP2);
  final RDF rdfRecordP2MultipleOccurrences = new RdfConversionUtils().convertStringToRdf(rdfStringP2MultipleOccurrences);
  final RDF rdfRecordP6 = new RdfConversionUtils().convertStringToRdf(rdfStringP6);
  final RDF rdfRecordP12 = new RdfConversionUtils().convertStringToRdf(rdfStringP12);

  @Autowired
  private PatternAnalysisServiceImpl patternAnalysisService;
  @Resource
  private ProblemPatternsRepositories problemPatternsRepositories;
  @Resource
  private ExecutionPointRepository executionPointRepository;
  @Resource
  private DatasetProblemPatternRepository datasetProblemPatternRepository;
  @Resource
  private RecordProblemPatternRepository recordProblemPatternRepository;
  @Resource
  private RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository;
  @Resource
  private RecordTitleRepository recordTitleRepository;

  PatternAnalysisServiceImplIT() throws IOException, SerializationException {
    //Required for the RDFs initializations
  }

  @BeforeEach
  public void cleanup() {
    executionPointRepository.deleteAll();
    datasetProblemPatternRepository.deleteAll();
    recordProblemPatternRepository.deleteAll();
    recordProblemPatternOccurrenceRepository.deleteAll();
    recordTitleRepository.deleteAll();
  }

  @Test
  void initializePatternAnalysisExecution() {
    final LocalDateTime now = LocalDateTime.now();
    final ExecutionPoint executionPoint1 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL,
        now);
    assertEquals(1, executionPoint1.getExecutionPointId());
    assertEquals("1", executionPoint1.getDatasetId());
    assertEquals(Step.VALIDATE_INTERNAL.name(), executionPoint1.getExecutionStep());
    assertEquals(now, executionPoint1.getExecutionTimestamp());

    //Second time should give back the exact same object
    final ExecutionPoint executionPoint2 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL,
        now);
    assertEquals(1, executionPoint2.getExecutionPointId());
    assertEquals("1", executionPoint2.getDatasetId());
    assertEquals(Step.VALIDATE_INTERNAL.name(), executionPoint2.getExecutionStep());
    assertEquals(now, executionPoint2.getExecutionTimestamp());
  }

  @Test
  void generateRecordPatternAnalysisTest() {
    //Insert a problem pattern
    final LocalDateTime nowP2 = LocalDateTime.now();
    final ExecutionPoint executionPoint1 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL,
        nowP2);
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint1, rdfRecordP2);
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternOccurrenceRepository.count());

    //Same insertion should fail
    assertThrows(DataIntegrityViolationException.class,
        () -> patternAnalysisService.generateRecordPatternAnalysis(executionPoint1, rdfRecordP2));
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternOccurrenceRepository.count());

    //Insert another problem pattern
    final LocalDateTime nowP6 = LocalDateTime.now();
    final ExecutionPoint executionPoint2 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL,
        nowP6);
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint2, rdfRecordP6);
    assertEquals(2, executionPointRepository.count());
    assertEquals(2L * ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(2, recordProblemPatternRepository.count());
    assertEquals(2, recordProblemPatternOccurrenceRepository.count());

    //Get dataset pattern analysis and check results
    final Optional<DatasetProblemPatternAnalysis<Step>> datasetPatternAnalysis = patternAnalysisService.getDatasetPatternAnalysis(
        "1", Step.VALIDATE_INTERNAL, nowP6);
    assertTrue(datasetPatternAnalysis.isPresent());
    assertEquals(1, datasetPatternAnalysis.get().getProblemPatternList().size());
    final ProblemPattern problemPatternP6 = getProblemPatternFromDatasetPatternAnalysis(datasetPatternAnalysis,
        ProblemPatternDescription.P6);

    assertEquals(1, problemPatternP6.getRecordAnalysisList().size());
    assertEquals(1, problemPatternP6.getRecordAnalysisList().get(0).getProblemOccurrenceList().size());
    assertTrue(isNotBlank(problemPatternP6.getRecordAnalysisList().get(0).getProblemOccurrenceList().get(0).getMessageReport()));

    //Empty result
    assertTrue(patternAnalysisService.getDatasetPatternAnalysis("1", Step.HARVEST_ZIP, nowP6).isEmpty());
  }

  @Test
  void generateRecordPatternAnalysis_withTooManySamePatternTypeOccurencesTest() {
    //We just want 1 occurrence
    final PatternAnalysisServiceImpl patternAnalysisService = new PatternAnalysisServiceImpl(problemPatternsRepositories, 1, 1);
    //Insert a problem pattern
    final LocalDateTime nowP2 = LocalDateTime.now();
    final ExecutionPoint executionPoint1 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL,
        nowP2);
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint1, rdfRecordP2MultipleOccurrences);
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternOccurrenceRepository.count());
  }

  @Test
  void generateRecordPatternAnalysis_withTooManySamePatternRecordsTest() {
    //We just want 1 record
    final PatternAnalysisServiceImpl patternAnalysisService = new PatternAnalysisServiceImpl(problemPatternsRepositories, 1, 1);

    final LocalDateTime nowP6 = LocalDateTime.now();
    final ExecutionPoint executionPoint1 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL,
        nowP6);
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint1, rdfRecordP6);

    //Update about to pretend a different record
    final String about = rdfRecordP6.getProvidedCHOList().get(0).getAbout();
    rdfRecordP6.getProvidedCHOList().get(0).setAbout(about + "2");
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint1, rdfRecordP6);

    //Get dataset pattern analysis and check results
    final Optional<DatasetProblemPatternAnalysis<Step>> datasetPatternAnalysis = patternAnalysisService.getDatasetPatternAnalysis(
        "1", Step.VALIDATE_INTERNAL, nowP6);
    assertTrue(datasetPatternAnalysis.isPresent());
    assertEquals(1, datasetPatternAnalysis.get().getProblemPatternList().size());
    final ProblemPattern problemPatternP6 = getProblemPatternFromDatasetPatternAnalysis(datasetPatternAnalysis,
        ProblemPatternDescription.P6);

    assertEquals(1, problemPatternP6.getRecordAnalysisList().size());
    assertEquals(1, problemPatternP6.getRecordAnalysisList().get(0).getProblemOccurrenceList().size());
    assertTrue(isNotBlank(problemPatternP6.getRecordAnalysisList().get(0).getProblemOccurrenceList().get(0).getMessageReport()));
    assertEquals(2, problemPatternP6.getRecordOccurrences()); //We count more than what we store
  }

  @Test
  void generateRecordPatternAnalysis_StringPayloadTest() throws Exception {
    final LocalDateTime nowP2 = LocalDateTime.now();
    final ExecutionPoint executionPoint1 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL,
        nowP2);
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint1, rdfStringP2);
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternOccurrenceRepository.count());

    //Invalid String payload
    assertThrows(PatternAnalysisException.class,
        () -> patternAnalysisService.generateRecordPatternAnalysis(executionPoint1, "Invalid String"));
  }

  @Test
  void getRecordPatternAnalysisTest() {
    final LocalDateTime nowP2 = LocalDateTime.now();
    final ExecutionPoint executionPoint1 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL,
        nowP2);
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint1, rdfRecordP2);
    //We should be getting this from the database
    List<ProblemPattern> problemPatternsRecord1 = patternAnalysisService.getRecordPatternAnalysis(rdfRecordP2);
    assertFalse(problemPatternsRecord1.isEmpty());

    //It does NOT exist in the database but we should get the on the fly version
    List<ProblemPattern> problemPatternsRecord2 = patternAnalysisService.getRecordPatternAnalysis(rdfRecordP6);
    assertFalse(problemPatternsRecord2.isEmpty());

    //Verify that we get empty list with no problem patterns encountered
    final List<ProblemPattern> recordPatternAnalysis = patternAnalysisService.getRecordPatternAnalysis(rdfRecordNoProblems);
    assertNotNull(recordPatternAnalysis);
    assertEquals(0, recordPatternAnalysis.size());
  }

  @Test
  void generateRecordPatternAnalysis_P12_with_longer_than_varcharTest() {
    final LocalDateTime now = LocalDateTime.now();
    final ExecutionPoint executionPoint = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL,
        now);
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint, rdfRecordP12);
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(2, recordProblemPatternOccurrenceRepository.count());

    //Check titles
    assertEquals(4, recordTitleRepository.count());
  }

  @Test
  void generateRecordPatternAnalysis_multipleRecords_and_global_patterns() throws SerializationException {
    //Insert a problem pattern
    final LocalDateTime nowP2 = LocalDateTime.now();
    final ExecutionPoint executionPoint1 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL,
        nowP2);
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint1, rdfRecordP2);

    //Clone same item
    final RDF rdfRecordP2Clone = new RdfConversionUtils().convertStringToRdf(rdfStringP2);
    rdfRecordP2Clone.getProvidedCHOList().get(0).setAbout(rdfRecordP2Clone.getProvidedCHOList().get(0).getAbout() + "Clone");
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint1, rdfRecordP2Clone);

    patternAnalysisService.finalizeDatasetPatternAnalysis(executionPoint1);

    //Get dataset pattern analysis and check results
    final Optional<DatasetProblemPatternAnalysis<Step>> datasetPatternAnalysis = patternAnalysisService.getDatasetPatternAnalysis(
        "1", Step.VALIDATE_INTERNAL, nowP2);
    assertTrue(datasetPatternAnalysis.isPresent());
    assertEquals(2, datasetPatternAnalysis.get().getProblemPatternList().size());
    final ProblemPattern problemPatternP2 = getProblemPatternFromDatasetPatternAnalysis(datasetPatternAnalysis,
        ProblemPatternDescription.P2);

    assertEquals(2, problemPatternP2.getRecordAnalysisList().size());
    assertEquals(1, problemPatternP2.getRecordAnalysisList().get(0).getProblemOccurrenceList().size());
    assertTrue(isNotBlank(problemPatternP2.getRecordAnalysisList().get(0).getProblemOccurrenceList().get(0).getMessageReport()));

    //Check global pattern
    final ProblemPattern problemPatternP1 = getProblemPatternFromDatasetPatternAnalysis(datasetPatternAnalysis,
        ProblemPatternDescription.P1);

    assertEquals(2, problemPatternP1.getRecordAnalysisList().size());
    assertEquals(2,
        problemPatternP1.getRecordAnalysisList().get(0).getProblemOccurrenceList().get(0).getAffectedRecordIds().size());
    assertTrue(isNotBlank(problemPatternP1.getRecordAnalysisList().get(0).getProblemOccurrenceList().get(0).getMessageReport()));
  }

  @Test
  void finalizeDatasetPatternAnalysisTest() {
    final LocalDateTime now = LocalDateTime.now();
    final ExecutionPoint executionPoint = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL,
        now);

    final RecordTitle recordTitle1A = new RecordTitle(
        new RecordTitleCompositeKey(executionPoint.getExecutionPointId(), "recordId1", "titleA"), executionPoint);
    final RecordTitle recordTitle2A = new RecordTitle(
        new RecordTitleCompositeKey(executionPoint.getExecutionPointId(), "recordId2", "titleA"), executionPoint);
    final RecordTitle recordTitle2B = new RecordTitle(
        new RecordTitleCompositeKey(executionPoint.getExecutionPointId(), "recordId2", "titleB"), executionPoint);
    recordTitleRepository.save(recordTitle1A);
    recordTitleRepository.save(recordTitle2A);
    recordTitleRepository.save(recordTitle2B);

    patternAnalysisService.finalizeDatasetPatternAnalysis(executionPoint);
    assertEquals(2, recordProblemPatternRepository.count());
    assertEquals(2, recordProblemPatternOccurrenceRepository.count());
    assertEquals(0, recordTitleRepository.count());
  }

  @NotNull
  private ProblemPattern getProblemPatternFromDatasetPatternAnalysis(
      Optional<DatasetProblemPatternAnalysis<Step>> datasetPatternAnalysis, ProblemPatternDescription problemPatternDescription) {
    return datasetPatternAnalysis.map(DatasetProblemPatternAnalysis::getProblemPatternList).stream().flatMap(Collection::stream)
                                 .filter(
                                     problemPattern -> problemPattern.getProblemPatternDescription() == problemPatternDescription)
                                 .findFirst().orElseThrow();
  }
}