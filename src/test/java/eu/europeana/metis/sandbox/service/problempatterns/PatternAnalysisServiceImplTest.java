package eu.europeana.metis.sandbox.service.problempatterns;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternJdbcRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternOccurrenceRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordTitleJdbcRepository;
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
import java.util.List;
import java.util.Optional;
import javax.annotation.Resource;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@ExtendWith(SpringExtension.class)
@Configuration
@EnableAutoConfiguration(exclude = {EmbeddedMongoAutoConfiguration.class, MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class})
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "eu.europeana.metis.sandbox.repository.problempatterns")
@EntityScan(basePackages = "eu.europeana.metis.sandbox.entity.problempatterns")
@ComponentScan("eu.europeana.metis.sandbox.service.problempatterns")
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=none"}) //We do not want hibernate creating the db
@Sql("classpath:database/schema_problem_patterns.sql") //We want the sql script to create the db
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class PatternAnalysisServiceImplTest {

  final String rdfStringP2 = IOUtils.toString(
      new FileInputStream("src/test/resources/record.problempatterns/europeana_record_with_P2.xml"), StandardCharsets.UTF_8);
  final String rdfStringP2MultipleOccurences = IOUtils.toString(
      new FileInputStream("src/test/resources/record.problempatterns/europeana_record_with_P2_multiple.xml"),
      StandardCharsets.UTF_8);
  final String rdfStringP6 = IOUtils.toString(
      new FileInputStream("src/test/resources/record.problempatterns/europeana_record_with_P6.xml"), StandardCharsets.UTF_8);
  final String rdfStringP12 = IOUtils.toString(
      new FileInputStream("src/test/resources/record.problempatterns/europeana_record_with_P12.xml"), StandardCharsets.UTF_8);

  final RDF rdfRecordP2 = new RdfConversionUtils().convertStringToRdf(rdfStringP2);
  final RDF rdfRecordP2MultipleOccurrences = new RdfConversionUtils().convertStringToRdf(rdfStringP2MultipleOccurences);
  final RDF rdfRecordP6 = new RdfConversionUtils().convertStringToRdf(rdfStringP6);
  final RDF rdfRecordP12 = new RdfConversionUtils().convertStringToRdf(rdfStringP12);

  @Autowired
  private PatternAnalysisServiceImpl patternAnalysisService;
  @Resource
  private ExecutionPointRepository executionPointRepository;
  @Resource
  private DatasetProblemPatternRepository datasetProblemPatternRepository;
  @Resource
  private DatasetProblemPatternJdbcRepository datasetProblemPatternJdbcRepository;
  @Resource
  private RecordProblemPatternRepository recordProblemPatternRepository;
  @Resource
  private RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository;
  @Resource
  private RecordTitleRepository recordTitleRepository;
  @Resource
  private RecordTitleJdbcRepository recordTitleJdbcRepository;

  PatternAnalysisServiceImplTest() throws IOException, SerializationException {
  }

  @BeforeAll
  public static void setErrorLogging() {
    //Disable debug logs
    LoggingSystem.get(ClassLoader.getSystemClassLoader()).setLogLevel(Logger.ROOT_LOGGER_NAME, LogLevel.INFO);
  }

  @Test
  void initializePatternAnalysisExecution() {
    final LocalDateTime now = LocalDateTime.now();
    final ExecutionPoint executionPoint = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL, now);
    assertEquals(1, executionPoint.getExecutionPointId());
    assertEquals("1", executionPoint.getDatasetId());
    assertEquals(Step.VALIDATE_INTERNAL.name(), executionPoint.getExecutionStep());
    assertEquals(now, executionPoint.getExecutionTimestamp());
  }

  @Test
  void generateRecordPatternAnalysisTest() {
    //Insert a problem pattern
    final LocalDateTime nowP2 = LocalDateTime.now();
    final ExecutionPoint executionPoint1 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL, nowP2);
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
    final ExecutionPoint executionPoint2 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL, nowP6);
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
    final ProblemPattern problemPatternP6 = datasetPatternAnalysis.get().getProblemPatternList().stream()
                                                                  .filter(problemPattern ->
                                                                      problemPattern.getProblemPatternDescription()
                                                                          == ProblemPatternDescription.P6).findFirst()
                                                                  .orElseThrow();

    assertEquals(1, problemPatternP6.getRecordAnalysisList().size());
    assertEquals(1, problemPatternP6.getRecordAnalysisList().get(0).getProblemOccurrenceList().size());
    assertTrue(isNotBlank(problemPatternP6.getRecordAnalysisList().get(0).getProblemOccurrenceList().get(0).getMessageReport()));

    //Empty result
    assertTrue(patternAnalysisService.getDatasetPatternAnalysis("1", Step.HARVEST_ZIP, nowP6).isEmpty());
  }

  @Test
  void generateRecordPatternAnalysis_withTooManySamePatternTypeOccurencesTest() {
    //We just want 1 occurrence
    final PatternAnalysisServiceImpl patternAnalysisService = new PatternAnalysisServiceImpl(executionPointRepository,
        datasetProblemPatternRepository, datasetProblemPatternJdbcRepository, recordProblemPatternRepository, recordProblemPatternOccurrenceRepository,
        recordTitleRepository, recordTitleJdbcRepository, 1, 1);
    //Insert a problem pattern
    final LocalDateTime nowP2 = LocalDateTime.now();
    final ExecutionPoint executionPoint1 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL, nowP2);
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint1, rdfRecordP2MultipleOccurrences);
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternOccurrenceRepository.count());
  }

  @Test
  void generateRecordPatternAnalysis_withTooManySamePatternRecordsTest() {
    //We just want 1 record
    final PatternAnalysisServiceImpl patternAnalysisService = new PatternAnalysisServiceImpl(executionPointRepository,
        datasetProblemPatternRepository, datasetProblemPatternJdbcRepository, recordProblemPatternRepository, recordProblemPatternOccurrenceRepository,
        recordTitleRepository, recordTitleJdbcRepository, 1, 1);

    final LocalDateTime nowP6 = LocalDateTime.now();
    final ExecutionPoint executionPoint1 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL, nowP6);
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
    final ProblemPattern problemPatternP6 = datasetPatternAnalysis.get().getProblemPatternList().stream()
                                                                  .filter(problemPattern ->
                                                                      problemPattern.getProblemPatternDescription()
                                                                          == ProblemPatternDescription.P6).findFirst()
                                                                  .orElseThrow();

    assertEquals(1, problemPatternP6.getRecordAnalysisList().size());
    assertEquals(1, problemPatternP6.getRecordAnalysisList().get(0).getProblemOccurrenceList().size());
    assertTrue(isNotBlank(problemPatternP6.getRecordAnalysisList().get(0).getProblemOccurrenceList().get(0).getMessageReport()));
    assertEquals(2, problemPatternP6.getRecordOccurrences()); //We count more than what we store
  }

  @Test
  void generateRecordPatternAnalysis_StringPayloadTest() throws Exception {
    final LocalDateTime nowP2 = LocalDateTime.now();
    final ExecutionPoint executionPoint1 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL, nowP2);
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
    final ExecutionPoint executionPoint1 = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL, nowP2);
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint1, rdfRecordP2);
    //We should be getting this from the database
    List<ProblemPattern> problemPatternsRecord1 = patternAnalysisService.getRecordPatternAnalysis(rdfRecordP2);
    assertFalse(problemPatternsRecord1.isEmpty());

    //It does NOT exist in the database but we should get the on the fly version
    List<ProblemPattern> problemPatternsRecord2 = patternAnalysisService.getRecordPatternAnalysis(rdfRecordP6);
    assertFalse(problemPatternsRecord2.isEmpty());
  }

  @Test
  void generateRecordPatternAnalysis_P12_with_longer_than_varcharTest() {
    final LocalDateTime now = LocalDateTime.now();
    final ExecutionPoint executionPoint = patternAnalysisService.initializePatternAnalysisExecution("1", Step.VALIDATE_INTERNAL, now);
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint, rdfRecordP12);
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(2, recordProblemPatternOccurrenceRepository.count());

    //Check titles
    assertEquals(4, recordTitleRepository.count());
  }
}
