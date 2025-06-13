package eu.europeana.metis.sandbox.integration.repository.problempatterns;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPattern;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.integration.testcontainers.PostgresTestContainersConfiguration;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaRepositories(basePackages = "eu.europeana.metis.sandbox.repository.problempatterns")
@EntityScan(basePackages = "eu.europeana.metis.sandbox.entity.problempatterns")
@Import(PostgresTestContainersConfiguration.class)
class DatasetProblemPatternRepositoryIT {

  private ExecutionPointRepository executionPointRepository;
  private DatasetProblemPatternRepository datasetProblemPatternRepository;
  private EntityManager entityManager;

  @Autowired
  DatasetProblemPatternRepositoryIT(ExecutionPointRepository executionPointRepository,
      DatasetProblemPatternRepository datasetProblemPatternRepository, EntityManager entityManager) {
    this.executionPointRepository = executionPointRepository;
    this.datasetProblemPatternRepository = datasetProblemPatternRepository;
    this.entityManager = entityManager;
  }

  @AfterEach
  void cleanup() {
    datasetProblemPatternRepository.deleteAll();
    executionPointRepository.deleteAll();
  }

  @Test
  void upsertUpdateCounterTest() {
    ExecutionPoint executionPoint = insertValues();

    //Verify empty
    List<DatasetProblemPattern> problemPatterns = datasetProblemPatternRepository.findAll();
    assertEquals(0, problemPatterns.size());

    //Upsert with 0
    datasetProblemPatternRepository.upsertCounter(executionPoint.getExecutionPointId(), "P1", 0);
    syncAndReload();
    problemPatterns = datasetProblemPatternRepository.findAll();
    assertEquals(1, problemPatterns.size());
    assertEquals(0, getOccurrences(problemPatterns, "P1"));

    //Upsert with 1, different pattern
    datasetProblemPatternRepository.upsertCounter(executionPoint.getExecutionPointId(), "P2", 1);
    syncAndReload();
    problemPatterns = datasetProblemPatternRepository.findAll();
    assertEquals(2, problemPatterns.size());
    assertEquals(1, getOccurrences(problemPatterns, "P2"));

    //Upsert with 1, same pattern
    datasetProblemPatternRepository.upsertCounter(executionPoint.getExecutionPointId(), "P2", 1);
    syncAndReload();
    problemPatterns = datasetProblemPatternRepository.findAll();
    assertEquals(2, problemPatterns.size());
    assertEquals(2, getOccurrences(problemPatterns, "P2"));

    //Upsert with 5, same pattern
    datasetProblemPatternRepository.upsertCounter(executionPoint.getExecutionPointId(), "P2", 5);
    syncAndReload();
    problemPatterns = datasetProblemPatternRepository.findAll();
    assertEquals(2, problemPatterns.size());
    assertEquals(7, getOccurrences(problemPatterns, "P2"));
  }

  @NotNull
  private Integer getOccurrences(List<DatasetProblemPattern> problemPatterns, String patternIdString) {
    return problemPatterns.stream().filter(
                              datasetProblemPattern -> datasetProblemPattern.getDatasetProblemPatternCompositeKey().getPatternId().equals(
                                  patternIdString))
                          .map(DatasetProblemPattern::getRecordOccurrences).findFirst().orElse(-1);
  }

  private void syncAndReload() {
    entityManager.flush();
    entityManager.clear();
  }

  private ExecutionPoint insertValues() {
    ExecutionPoint executionPoint = new ExecutionPoint();
    executionPoint.setDatasetId("1");
    executionPoint.setExecutionName("VALIDATION_EXTERNAL");
    executionPoint.setExecutionTimestamp(LocalDateTime.parse("2022-03-22T10:10:10.100"));
    return executionPointRepository.save(executionPoint);
  }
}
