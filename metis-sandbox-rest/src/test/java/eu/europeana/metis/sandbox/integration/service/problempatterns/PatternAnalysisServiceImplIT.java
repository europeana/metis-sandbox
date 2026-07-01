package eu.europeana.metis.sandbox.integration.service.problempatterns;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.batch.FullBatchJobType;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitle;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitleCompositeKey;
import eu.europeana.metis.sandbox.integration.service.problempatterns.PatternAnalysisServiceImplIT.PatternAnalysisServiceImplMaxPatternsConfig;
import eu.europeana.metis.sandbox.integration.testcontainers.PostgresTestContainersConfiguration;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternOccurrenceRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordTitleRepository;
import eu.europeana.metis.sandbox.service.problempatterns.PatternAnalysisServiceImpl;
import eu.europeana.metis.sandbox.service.problempatterns.ProblemPatternDataCleaner;
import eu.europeana.metis.sandbox.service.problempatterns.ProblemPatternsRepositories;
import eu.europeana.metis.schema.convert.RdfConversionUtils;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.metis.schema.jibx.RDF;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.patternanalysis.view.DatasetProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemOccurrence;
import eu.europeana.patternanalysis.view.ProblemPattern;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import eu.europeana.patternanalysis.view.RecordAnalysis;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootTest(classes = PatternAnalysisServiceImpl.class)
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = "eu.europeana.metis.sandbox.repository.problempatterns")
@EntityScan(basePackages = "eu.europeana.metis.sandbox.entity.problempatterns")
@ComponentScan(basePackages = "eu.europeana.metis.sandbox.service.problempatterns")
@Import({PatternAnalysisServiceImplMaxPatternsConfig.class, PostgresTestContainersConfiguration.class})
class PatternAnalysisServiceImplIT {

  private final String rdfStringP2;

  private final RDF rdfRecordNoProblems;
  private final RDF rdfRecordP2;
  private final RDF rdfRecordP2MultipleOccurrences;
  private final RDF rdfRecordP6;
  private final RDF rdfRecordP12;

  private final PatternAnalysisServiceImpl patternAnalysisServiceImpl;
  private final PatternAnalysisServiceImpl patternAnalysisServiceMaxPatterns2;
  private final ProblemPatternsRepositories problemPatternsRepositories;
  private final ExecutionPointRepository executionPointRepository;
  private final DatasetProblemPatternRepository datasetProblemPatternRepository;
  private final RecordProblemPatternRepository recordProblemPatternRepository;
  private final RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository;
  private final RecordTitleRepository recordTitleRepository;
  private final ProblemPatternDataCleaner problemPatternDataCleaner;

  @Autowired
  public PatternAnalysisServiceImplIT(
      @Qualifier("patternAnalysisServiceImpl") PatternAnalysisServiceImpl patternAnalysisServiceImpl,
      @Qualifier("patternAnalysisServiceMaxPatterns2") PatternAnalysisServiceImpl patternAnalysisServiceMaxPatterns2,
      ProblemPatternsRepositories problemPatternsRepositories,
      ExecutionPointRepository executionPointRepository,
      DatasetProblemPatternRepository datasetProblemPatternRepository,
      RecordProblemPatternRepository recordProblemPatternRepository,
      RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository,
      RecordTitleRepository recordTitleRepository,
      ProblemPatternDataCleaner problemPatternDataCleaner
  ) throws IOException, SerializationException {

    this.patternAnalysisServiceImpl = patternAnalysisServiceImpl;
    this.patternAnalysisServiceMaxPatterns2 = patternAnalysisServiceMaxPatterns2;
    this.problemPatternsRepositories = problemPatternsRepositories;
    this.executionPointRepository = executionPointRepository;
    this.datasetProblemPatternRepository = datasetProblemPatternRepository;
    this.recordProblemPatternRepository = recordProblemPatternRepository;
    this.recordProblemPatternOccurrenceRepository = recordProblemPatternOccurrenceRepository;
    this.recordTitleRepository = recordTitleRepository;
    this.problemPatternDataCleaner = problemPatternDataCleaner;

    String rdfStringNoProblems = readTestResourceAsString("record/problempatterns/europeana_record_no_problem_patterns.xml");
    this.rdfStringP2 = readTestResourceAsString("record/problempatterns/europeana_record_with_P2.xml");
    String rdfStringP2MultipleOccurrences = readTestResourceAsString(
        "record/problempatterns/europeana_record_with_P2_multiple.xml");
    String rdfStringP6 = readTestResourceAsString("record/problempatterns/europeana_record_with_P6.xml");
    String rdfStringP12 = readTestResourceAsString("record/problempatterns/europeana_record_with_P12.xml");

    RdfConversionUtils rdfConversionUtils = new RdfConversionUtils();
    this.rdfRecordNoProblems = rdfConversionUtils.convertStringToRdf(rdfStringNoProblems);
    this.rdfRecordP2 = rdfConversionUtils.convertStringToRdf(rdfStringP2);
    this.rdfRecordP2MultipleOccurrences = rdfConversionUtils.convertStringToRdf(rdfStringP2MultipleOccurrences);
    this.rdfRecordP6 = rdfConversionUtils.convertStringToRdf(rdfStringP6);
    this.rdfRecordP12 = rdfConversionUtils.convertStringToRdf(rdfStringP12);
  }

  private static String readTestResourceAsString(String resourcePath) throws IOException {
    return new String(
        new ClassPathResource(resourcePath).getInputStream().readAllBytes(),
        StandardCharsets.UTF_8
    );
  }

  @AfterEach
  void cleanup() {
    problemPatternDataCleaner.deleteAll();
  }

  @Test
  void initializePatternAnalysisExecution() {
    final ExecutionPoint executionPoint1 = patternAnalysisServiceImpl.initializePatternAnalysisExecution(
        "1", FullBatchJobType.VALIDATE_INTERNAL);
    assertEquals("1", executionPoint1.getDatasetId());
    assertEquals(FullBatchJobType.VALIDATE_INTERNAL.name(), executionPoint1.getExecutionName());
    assertNotNull(executionPoint1.getExecutionTimestamp());
  }

  @Test
  void generateRecordPatternAnalysisTest() {
    //Insert a problem pattern
    final ExecutionPoint executionPoint1 = patternAnalysisServiceImpl.initializePatternAnalysisExecution(
        "1", FullBatchJobType.VALIDATE_INTERNAL);
    patternAnalysisServiceImpl.generateRecordPatternAnalysis(executionPoint1, rdfRecordP2);
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length,
        datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternOccurrenceRepository.count());

    //Same insertion should fail
    assertThrows(DataIntegrityViolationException.class,
        () -> patternAnalysisServiceImpl.generateRecordPatternAnalysis(executionPoint1, rdfRecordP2));
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternOccurrenceRepository.count());

    //Insert another problem pattern
    final ExecutionPoint executionPoint2 = patternAnalysisServiceImpl.initializePatternAnalysisExecution(
        "1", FullBatchJobType.VALIDATE_INTERNAL);
    patternAnalysisServiceImpl.generateRecordPatternAnalysis(executionPoint2, rdfRecordP6);
    assertEquals(2, executionPointRepository.count());
    assertEquals(2L * ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(2, recordProblemPatternRepository.count());
    assertEquals(2, recordProblemPatternOccurrenceRepository.count());

    //Get dataset pattern analysis and check results
    final DatasetProblemPatternAnalysis<FullBatchJobType> datasetPatternAnalysis = patternAnalysisServiceImpl.getDatasetPatternAnalysis(
        "1", FullBatchJobType.VALIDATE_INTERNAL, executionPoint2.getExecutionTimestamp()).orElseThrow();
    assertEquals(1, datasetPatternAnalysis.problemPatternList().size());
    final ProblemPattern problemPatternP6 = getProblemPatternFromDatasetPatternAnalysis(
        datasetPatternAnalysis, ProblemPatternDescription.P6);

    assertEquals(1, problemPatternP6.recordAnalysisList().size());
    assertEquals(1, problemPatternP6.recordAnalysisList().getFirst().problemOccurrenceList().size());
    assertTrue(isNotBlank(
        problemPatternP6.recordAnalysisList().getFirst().problemOccurrenceList().getFirst().messageReport()));

    //Empty result
    assertTrue(patternAnalysisServiceImpl.getDatasetPatternAnalysis("1", FullBatchJobType.HARVEST_FILE,
        executionPoint2.getExecutionTimestamp()).isEmpty());
  }

  @Test
  void generateRecordPatternAnalysis_withTooManySamePatternTypeOccurrencesTest() {
    //We just want 1 occurrence
    final PatternAnalysisServiceImpl patternAnalysisService = new PatternAnalysisServiceImpl(
        problemPatternsRepositories, 1, 1);
    //Insert a problem pattern
    final ExecutionPoint executionPoint1 = patternAnalysisService.initializePatternAnalysisExecution(
        "1", FullBatchJobType.VALIDATE_INTERNAL);
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint1,
        rdfRecordP2MultipleOccurrences);
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternOccurrenceRepository.count());
  }

  @Test
  void generateRecordPatternAnalysis_withTooManySamePatternRecordsTest() {
    //We just want 1 record
    final PatternAnalysisServiceImpl patternAnalysisService = new PatternAnalysisServiceImpl(
        problemPatternsRepositories, 1, 1);

    final ExecutionPoint executionPoint1 = patternAnalysisService.initializePatternAnalysisExecution(
        "1", FullBatchJobType.VALIDATE_INTERNAL);
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint1, rdfRecordP6);

    //Update about to pretend a different record
    final String about = rdfRecordP6.getProvidedCHOList().getFirst().getAbout();
    rdfRecordP6.getProvidedCHOList().getFirst().setAbout(about + "2");
    patternAnalysisService.generateRecordPatternAnalysis(executionPoint1, rdfRecordP6);

    //Get dataset pattern analysis and check results
    final DatasetProblemPatternAnalysis<FullBatchJobType> datasetPatternAnalysis = patternAnalysisService.getDatasetPatternAnalysis(
        "1", FullBatchJobType.VALIDATE_INTERNAL, executionPoint1.getExecutionTimestamp()).orElseThrow();
    assertEquals(1, datasetPatternAnalysis.problemPatternList().size());
    final ProblemPattern problemPatternP6 = getProblemPatternFromDatasetPatternAnalysis(
        datasetPatternAnalysis, ProblemPatternDescription.P6);

    assertEquals(1, problemPatternP6.recordAnalysisList().size());
    assertEquals(1, problemPatternP6.recordAnalysisList().getFirst().problemOccurrenceList().size());
    assertTrue(isNotBlank(
        problemPatternP6.recordAnalysisList().getFirst().problemOccurrenceList().getFirst().messageReport()));
    assertEquals(2, problemPatternP6.recordOccurrences()); //We count more than what we store
  }

  @Test
  void generateRecordPatternAnalysis_StringPayloadTest() throws Exception {
    final ExecutionPoint executionPoint1 = patternAnalysisServiceImpl.initializePatternAnalysisExecution(
        "1", FullBatchJobType.VALIDATE_INTERNAL);
    patternAnalysisServiceImpl.generateRecordPatternAnalysis(executionPoint1, rdfStringP2);
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternOccurrenceRepository.count());

    //Invalid String payload
    assertThrows(PatternAnalysisException.class,
        () -> patternAnalysisServiceImpl.generateRecordPatternAnalysis(executionPoint1, "Invalid String"));
  }

  @Test
  void getRecordPatternAnalysisTest() {
    final ExecutionPoint executionPoint1 = patternAnalysisServiceImpl.initializePatternAnalysisExecution(
        "1", FullBatchJobType.VALIDATE_INTERNAL);
    patternAnalysisServiceImpl.generateRecordPatternAnalysis(executionPoint1, rdfRecordP2);
    //We should be getting this from the database
    List<ProblemPattern> problemPatternsRecord1 = patternAnalysisServiceImpl.getRecordPatternAnalysis(rdfRecordP2);
    assertFalse(problemPatternsRecord1.isEmpty());

    //It does NOT exist in the database, but we should get the on the fly version
    List<ProblemPattern> problemPatternsRecord2 = patternAnalysisServiceImpl.getRecordPatternAnalysis(
        rdfRecordP6);
    assertFalse(problemPatternsRecord2.isEmpty());

    //Verify that we get an empty list with no problem patterns encountered
    final List<ProblemPattern> recordPatternAnalysis = patternAnalysisServiceImpl.getRecordPatternAnalysis(rdfRecordNoProblems);
    assertNotNull(recordPatternAnalysis);
    assertEquals(0, recordPatternAnalysis.size());
  }

  @Test
  void generateRecordPatternAnalysis_P12_with_longer_than_varcharTest() {
    final ExecutionPoint executionPoint = patternAnalysisServiceImpl.initializePatternAnalysisExecution(
        "1", FullBatchJobType.VALIDATE_INTERNAL);
    patternAnalysisServiceImpl.generateRecordPatternAnalysis(executionPoint, rdfRecordP12);
    assertEquals(1, executionPointRepository.count());
    assertEquals(ProblemPatternDescription.values().length, datasetProblemPatternRepository.count());
    assertEquals(1, recordProblemPatternRepository.count());
    assertEquals(3, recordProblemPatternOccurrenceRepository.count());

    //Check titles
    assertEquals(4, recordTitleRepository.count());
  }

  @Test
  void generateRecordPatternAnalysis_multipleRecords_and_global_patterns()
      throws SerializationException {
    //Insert a problem pattern
    final ExecutionPoint executionPoint1 = patternAnalysisServiceImpl.initializePatternAnalysisExecution(
        "1", FullBatchJobType.VALIDATE_INTERNAL);
    patternAnalysisServiceImpl.generateRecordPatternAnalysis(executionPoint1, rdfRecordP2);

    //Clone same item
    final RDF rdfRecordP2Clone = new RdfConversionUtils().convertStringToRdf(rdfStringP2);
    rdfRecordP2Clone.getProvidedCHOList().getFirst()
                    .setAbout(rdfRecordP2Clone.getProvidedCHOList().getFirst().getAbout() + "Clone");
    patternAnalysisServiceImpl.generateRecordPatternAnalysis(executionPoint1, rdfRecordP2Clone);
    patternAnalysisServiceImpl.finalizeDatasetPatternAnalysis(executionPoint1);

    //Get dataset pattern analysis and check results
    final DatasetProblemPatternAnalysis<FullBatchJobType> datasetPatternAnalysis = patternAnalysisServiceImpl.getDatasetPatternAnalysis(
        "1", FullBatchJobType.VALIDATE_INTERNAL, executionPoint1.getExecutionTimestamp()).orElseThrow();
    assertEquals(2, datasetPatternAnalysis.problemPatternList().size());
    final ProblemPattern problemPatternP2 = getProblemPatternFromDatasetPatternAnalysis(
        datasetPatternAnalysis, ProblemPatternDescription.P2);

    assertEquals(2, problemPatternP2.recordAnalysisList().size());
    assertEquals(1,
        problemPatternP2.recordAnalysisList().getFirst().problemOccurrenceList().size());
    assertTrue(isNotBlank(
        problemPatternP2.recordAnalysisList().getFirst().problemOccurrenceList().getFirst().messageReport()));

    //Check a global pattern
    final ProblemPattern problemPatternP1 = getProblemPatternFromDatasetPatternAnalysis(
        datasetPatternAnalysis, ProblemPatternDescription.P1);

    assertEquals(2, problemPatternP1.recordAnalysisList().size());
    assertEquals(2,
        problemPatternP1.recordAnalysisList().getFirst().problemOccurrenceList().getFirst().affectedRecordIds().size());
    assertTrue(isNotBlank(
        problemPatternP1.recordAnalysisList().getFirst().problemOccurrenceList().getFirst().messageReport()));
  }

  @Test
  void generateRecordPatternAnalysis_duplicatedIgnoringCaseTitle()
      throws SerializationException, IOException {
    //Insert a problem pattern
    final RDF rdfRecords1 = new RdfConversionUtils().convertStringToRdf(
        IOUtils.toString(
            new FileInputStream("src/test/resources/record/problempatterns/P1_lowercase_title.xml"), StandardCharsets.UTF_8));
    final RDF rdfRecords2 = new RdfConversionUtils().convertStringToRdf(
        IOUtils.toString(
            new FileInputStream("src/test/resources/record/problempatterns/P1_uppercase_title.xml"), StandardCharsets.UTF_8));

    final ExecutionPoint executionPoint1 = patternAnalysisServiceImpl.initializePatternAnalysisExecution(
        "1", FullBatchJobType.VALIDATE_INTERNAL);
    patternAnalysisServiceImpl.generateRecordPatternAnalysis(executionPoint1, rdfRecords1);
    patternAnalysisServiceImpl.generateRecordPatternAnalysis(executionPoint1, rdfRecords2);
    patternAnalysisServiceImpl.finalizeDatasetPatternAnalysis(executionPoint1);

    //Get dataset pattern analysis and check results
    final DatasetProblemPatternAnalysis<FullBatchJobType> datasetPatternAnalysis = patternAnalysisServiceImpl.getDatasetPatternAnalysis(
        "1", FullBatchJobType.VALIDATE_INTERNAL, executionPoint1.getExecutionTimestamp()).orElseThrow();
    assertEquals(1, datasetPatternAnalysis.problemPatternList().size());
    List<RecordAnalysis> recordAnalysisList = datasetPatternAnalysis.problemPatternList().getFirst()
                                                                    .recordAnalysisList();
    assertEquals(1, recordAnalysisList.size());
    RecordAnalysis recordAnalysis = recordAnalysisList.getFirst();
    assertTrue(recordAnalysis.recordId().equals("/21/_providedCHO_MHC_EMC_10_ms_06") ||
        recordAnalysis.recordId().equals("/21/_providedCHO_MHC_EMC_10_ms_07_jpg"));
    assertEquals(1, recordAnalysis.problemOccurrenceList().size());
    ProblemOccurrence occurrence = recordAnalysis.problemOccurrenceList().getFirst();
    assertTrue(occurrence.messageReport().equals("LOWERCASE or UPPERCASE title") ||
        occurrence.messageReport().equals("lowercase or uppercase title"));
    assertTrue(
        occurrence.affectedRecordIds().containsAll(List.of("/21/_providedCHO_MHC_EMC_10_ms_06",
            "/21/_providedCHO_MHC_EMC_10_ms_07_jpg")));
    assertTrue(List.of("/21/_providedCHO_MHC_EMC_10_ms_06",
        "/21/_providedCHO_MHC_EMC_10_ms_07_jpg").containsAll(occurrence.affectedRecordIds()));
  }

  @Test
  void finalizeDatasetPatternAnalysisTest() {
    final ExecutionPoint executionPoint = patternAnalysisServiceMaxPatterns2.initializePatternAnalysisExecution(
        "1", FullBatchJobType.VALIDATE_INTERNAL);

    final RecordTitle recordTitle1A = new RecordTitle(
        new RecordTitleCompositeKey(
            executionPoint.getExecutionPointId(), "recordId1", "titleA"), executionPoint);
    final RecordTitle recordTitle2A = new RecordTitle(
        new RecordTitleCompositeKey(executionPoint.getExecutionPointId(), "recordId2", "titleA"), executionPoint);
    final RecordTitle recordTitle3A = new RecordTitle(
        new RecordTitleCompositeKey(executionPoint.getExecutionPointId(), "recordId3", "titleA"), executionPoint);

    final RecordTitle recordTitle4B = new RecordTitle(
        new RecordTitleCompositeKey(executionPoint.getExecutionPointId(), "recordId4", "titleB"), executionPoint);
    final RecordTitle recordTitle5B = new RecordTitle(
        new RecordTitleCompositeKey(executionPoint.getExecutionPointId(), "recordId5", "titleB"), executionPoint);

    final RecordTitle recordTitle6C = new RecordTitle(
        new RecordTitleCompositeKey(executionPoint.getExecutionPointId(), "recordId6", "titleC"), executionPoint);
    final RecordTitle recordTitle7C = new RecordTitle(
        new RecordTitleCompositeKey(executionPoint.getExecutionPointId(), "recordId7", "titleC"), executionPoint);
    recordTitleRepository.save(recordTitle1A);
    recordTitleRepository.save(recordTitle2A);
    recordTitleRepository.save(recordTitle3A);
    recordTitleRepository.save(recordTitle4B);
    recordTitleRepository.save(recordTitle5B);
    recordTitleRepository.save(recordTitle6C);
    recordTitleRepository.save(recordTitle7C);

    patternAnalysisServiceMaxPatterns2.finalizeDatasetPatternAnalysis(executionPoint);
    //There should be 4 entries. 2 distinct titles, titles are up to 2recordIds therefore 2(titleA)+2(titleB)=4
    assertEquals(4, recordProblemPatternRepository.count());
    assertEquals(4, recordProblemPatternOccurrenceRepository.count());
    assertEquals(0, recordTitleRepository.count());
  }

  @NotNull
  private ProblemPattern getProblemPatternFromDatasetPatternAnalysis(
      DatasetProblemPatternAnalysis<FullBatchJobType> datasetPatternAnalysis,
      ProblemPatternDescription problemPatternDescription) {
    return Optional.of(datasetPatternAnalysis.problemPatternList()).stream()
                   .flatMap(Collection::stream)
                   .filter(problemPattern -> problemPattern.problemPatternDescription()
                       == problemPatternDescription)
                   .findFirst().orElseThrow();
  }

  @TestConfiguration
  static class PatternAnalysisServiceImplMaxPatternsConfig {

    @Bean
    PatternAnalysisServiceImpl patternAnalysisServiceMaxPatterns2(
        ProblemPatternsRepositories problemPatternsRepositories) {
      return new PatternAnalysisServiceImpl(problemPatternsRepositories, 2, 2);
    }
  }
}
