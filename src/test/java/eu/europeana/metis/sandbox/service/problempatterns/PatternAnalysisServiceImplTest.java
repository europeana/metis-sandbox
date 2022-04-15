package eu.europeana.metis.sandbox.service.problempatterns;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternOccurenceRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternRepository;
import eu.europeana.metis.schema.convert.RdfConversionUtils;
import eu.europeana.metis.schema.jibx.RDF;
import eu.europeana.patternanalysis.view.DatasetProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemPattern;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.annotation.Resource;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
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
@EnableAutoConfiguration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "eu.europeana.metis.sandbox.repository.problempatterns")
@EntityScan(basePackages = "eu.europeana.metis.sandbox.entity.problempatterns")
@ComponentScan("eu.europeana.metis.sandbox.service.problempatterns")
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=none"})
@Sql("classpath:database/schema_problem_patterns.sql")
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class PatternAnalysisServiceImplTest {

  @Autowired
  private PatternAnalysisServiceImpl patternAnalysisService;
  @Resource
  private ExecutionPointRepository executionPointRepository;
  @Resource
  private DatasetProblemPatternRepository datasetProblemPatternRepository;
  @Resource
  private RecordProblemPatternRepository recordProblemPatternRepository;
  @Resource
  private RecordProblemPatternOccurenceRepository recordProblemPatternOccurenceRepository;

//  @BeforeEach
//  void beforeEach() {
//    recordProblemPatternOccurenceRepository.deleteAll();
//    recordProblemPatternRepository.deleteAll();
//    datasetProblemPatternRepository.deleteAll();
//    executionPointRepository.deleteAll();
//  }

  @Test
  void generateRecordPatternAnalysisTest() throws Exception {
    //Insert a problem pattern
    String rdfStringP2 = IOUtils.toString(
        new FileInputStream("src/test/resources/record.problempatterns/europeana_record_with_P2.xml"),
        StandardCharsets.UTF_8);
    final RDF rdfRecordP2 = new RdfConversionUtils().convertStringToRdf(rdfStringP2);

    final LocalDateTime nowP2 = LocalDateTime.now();
    patternAnalysisService.generateRecordPatternAnalysis("1", Step.VALIDATE_INTERNAL, nowP2, rdfRecordP2);
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternOccurenceRepository.count());

    //Same insertion should fail
    assertThrows(DataIntegrityViolationException.class,
        () -> patternAnalysisService.generateRecordPatternAnalysis("1", Step.VALIDATE_INTERNAL, nowP2, rdfRecordP2));
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternOccurenceRepository.count());

    //Insert another problem pattern
    String rdfStringP6 = IOUtils.toString(
        new FileInputStream("src/test/resources/record.problempatterns/europeana_record_with_P6.xml"),
        StandardCharsets.UTF_8);
    final RDF rdfRecordP6 = new RdfConversionUtils().convertStringToRdf(rdfStringP6);

    final LocalDateTime nowP6 = LocalDateTime.now();
    patternAnalysisService.generateRecordPatternAnalysis("1", Step.VALIDATE_INTERNAL, nowP6, rdfRecordP6);
    assertEquals(2, executionPointRepository.count());
    assertEquals(2L * ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(2, recordProblemPatternRepository.count());
    assertEquals(2, recordProblemPatternOccurenceRepository.count());

    //Get dataset pattern analysis and check results
    final Optional<DatasetProblemPatternAnalysis<Step>> datasetPatternAnalysis = patternAnalysisService.getDatasetPatternAnalysis(
        "1", Step.VALIDATE_INTERNAL, nowP6);
    assertTrue(datasetPatternAnalysis.isPresent());
    assertEquals(ProblemPatternDescription.values().length, datasetPatternAnalysis.get().getProblemPatternList().size());
    final ProblemPattern problemPatternP6 = datasetPatternAnalysis.get().getProblemPatternList().stream()
                                                                  .filter(problemPattern ->
                                                                      problemPattern.getProblemPatternDescription()
                                                                          == ProblemPatternDescription.P6).findFirst()
                                                                  .orElseThrow();

    assertEquals(1, problemPatternP6.getRecordAnalysisList().size());
    assertEquals(1, problemPatternP6.getRecordAnalysisList().get(0).getProblemOccurenceList().size());
    assertTrue(isNotBlank(problemPatternP6.getRecordAnalysisList().get(0).getProblemOccurenceList().get(0).getMessageReport()));

    //Empty result
    assertTrue(patternAnalysisService.getDatasetPatternAnalysis("1", Step.HARVEST_ZIP, nowP6).isEmpty());
  }

  @Test
  void generateRecordPatternAnalysis_withTooManySamePatternTypeOccurencesTest() throws Exception {
    //We just want 1 occurence
    final PatternAnalysisServiceImpl patternAnalysisService = new PatternAnalysisServiceImpl(executionPointRepository,
        datasetProblemPatternRepository, recordProblemPatternRepository, recordProblemPatternOccurenceRepository, 1, 1);
    //Insert a problem pattern
    final String rdfStringP2 = IOUtils.toString(
        new FileInputStream("src/test/resources/record.problempatterns/europeana_record_with_P2_multiple.xml"),
        StandardCharsets.UTF_8);
    final RDF rdfRecordP2 = new RdfConversionUtils().convertStringToRdf(rdfStringP2);
    final LocalDateTime nowP2 = LocalDateTime.now();
    patternAnalysisService.generateRecordPatternAnalysis("1", Step.VALIDATE_INTERNAL, nowP2, rdfRecordP2);
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternOccurenceRepository.count());
  }

  @Test
  void generateRecordPatternAnalysis_withTooManySamePatternRecordsTest() throws Exception {
    //We just want 1 record
    final PatternAnalysisServiceImpl patternAnalysisService = new PatternAnalysisServiceImpl(executionPointRepository,
        datasetProblemPatternRepository, recordProblemPatternRepository, recordProblemPatternOccurenceRepository, 1, 1);

    String rdfStringP6 = IOUtils.toString(
        new FileInputStream("src/test/resources/record.problempatterns/europeana_record_with_P6.xml"),
        StandardCharsets.UTF_8);
    final RDF rdfRecordP6 = new RdfConversionUtils().convertStringToRdf(rdfStringP6);
    final LocalDateTime nowP6 = LocalDateTime.now();
    patternAnalysisService.generateRecordPatternAnalysis("1", Step.VALIDATE_INTERNAL, nowP6, rdfRecordP6);

    //Update about to pretend a different record
    final String about = rdfRecordP6.getProvidedCHOList().get(0).getAbout();
    rdfRecordP6.getProvidedCHOList().get(0).setAbout(about + "2");
    patternAnalysisService.generateRecordPatternAnalysis("1", Step.VALIDATE_INTERNAL, nowP6, rdfRecordP6);

    //Get dataset pattern analysis and check results
    final Optional<DatasetProblemPatternAnalysis<Step>> datasetPatternAnalysis = patternAnalysisService.getDatasetPatternAnalysis(
        "1", Step.VALIDATE_INTERNAL, nowP6);
    assertTrue(datasetPatternAnalysis.isPresent());
    assertEquals(ProblemPatternDescription.values().length, datasetPatternAnalysis.get().getProblemPatternList().size());
    final ProblemPattern problemPatternP6 = datasetPatternAnalysis.get().getProblemPatternList().stream()
                                                                  .filter(problemPattern ->
                                                                      problemPattern.getProblemPatternDescription()
                                                                          == ProblemPatternDescription.P6).findFirst()
                                                                  .orElseThrow();

    assertEquals(1, problemPatternP6.getRecordAnalysisList().size());
    assertEquals(1, problemPatternP6.getRecordAnalysisList().get(0).getProblemOccurenceList().size());
    assertTrue(isNotBlank(problemPatternP6.getRecordAnalysisList().get(0).getProblemOccurenceList().get(0).getMessageReport()));
    assertEquals(2, problemPatternP6.getRecordOccurences()); //We count more than what we store
  }
}