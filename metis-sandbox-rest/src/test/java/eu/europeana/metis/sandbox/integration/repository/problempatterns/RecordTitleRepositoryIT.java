package eu.europeana.metis.sandbox.integration.repository.problempatterns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitle;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordTitleCompositeKey;
import eu.europeana.metis.sandbox.integration.testcontainers.PostgresTestContainersConfiguration;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordTitleRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaRepositories(basePackages = "eu.europeana.metis.sandbox.repository.problempatterns")
@EntityScan(basePackages = "eu.europeana.metis.sandbox.entity.problempatterns")
@Import(PostgresTestContainersConfiguration.class)
class RecordTitleRepositoryIT {

  private final RecordTitleRepository recordTitleRepository;
  private final ExecutionPointRepository executionPointRepository;

  @Autowired
  public RecordTitleRepositoryIT(RecordTitleRepository recordTitleRepository,
      ExecutionPointRepository executionPointRepository) {
    this.recordTitleRepository = recordTitleRepository;
    this.executionPointRepository = executionPointRepository;
  }

  @AfterEach
  void cleanup() {
    recordTitleRepository.deleteAll();
    executionPointRepository.deleteAll();
  }

  @Test
  void deleteRedundantRecordTitlesTest() {
    ExecutionPoint executionPoint = insertValues();
    long executionPointCount = executionPointRepository.count();
    List<RecordTitle> recordTitles = recordTitleRepository.findAll();

    assertEquals(1, executionPointCount);
    assertEquals(5, recordTitles.size());
    recordTitleRepository.deleteRedundantRecordTitles(executionPoint.getExecutionPointId());
    executionPointCount = executionPointRepository.count();
    recordTitles = recordTitleRepository.findAll();

    assertEquals(2, recordTitles.size());
    assertEquals(1, executionPointCount);
    assertTrue(
        recordTitles.stream().allMatch(recordTitle -> recordTitle.getRecordTitleCompositeKey().getTitle().equals("titleA")));
  }

  private ExecutionPoint insertValues() {
    ExecutionPoint executionPoint = new ExecutionPoint();
    executionPoint.setDatasetId("1");
    executionPoint.setExecutionName("VALIDATION_EXTERNAL");
    executionPoint.setExecutionTimestamp(LocalDateTime.parse("2022-03-22T10:10:10.100"));
    ExecutionPoint savedExecutionPoint = executionPointRepository.save(executionPoint);
    recordTitleRepository.saveAll(List.of(
        new RecordTitle(new RecordTitleCompositeKey(1, "recordId1", "titleA"), savedExecutionPoint),
        new RecordTitle(new RecordTitleCompositeKey(1, "recordId1", "titleS"), savedExecutionPoint),
        new RecordTitle(new RecordTitleCompositeKey(1, "recordId1", "Some ValueC"), savedExecutionPoint),
        new RecordTitle(new RecordTitleCompositeKey(1, "recordId2", "titleA"), savedExecutionPoint),
        new RecordTitle(new RecordTitleCompositeKey(1, "recordId2", "titleB"), savedExecutionPoint)
    ));
    return savedExecutionPoint;
  }
}
