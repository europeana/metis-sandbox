package eu.europeana.metis.sandbox.integration.repository.problempatterns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.jdbc.JdbcTestUtils.deleteFromTables;

import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPattern;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPatternCompositeKey;
import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternJdbcRepository;
import eu.europeana.metis.sandbox.test.utils.PostgresTestContainersConfiguration;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;

@ExtendWith(SpringExtension.class)
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) //Do not allow JdbcTest to replace the Datasource
@ContextConfiguration(classes = DatasetProblemPatternJdbcRepository.class)
@Import({PostgresTestContainersConfiguration.class})
class DatasetProblemPatternJdbcRepositoryIT {

  @BeforeAll
  static void beforeAll() {
    //Sandbox specific properties
    PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.jdbcUrl", PostgreSQLContainer::getJdbcUrl);
    PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.username", PostgreSQLContainer::getUsername);
    PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.password", PostgreSQLContainer::getPassword);
    PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.driverClassName", container -> "org.postgresql.Driver");

    PostgresTestContainersConfiguration.runScripts(List.of(
        "database/schema_problem_patterns_drop.sql", "database/schema_problem_patterns.sql"
    ));
  }

  public static final String SELECT_DATASET_PROBLEM_PATTERN_QUERY = "SELECT * FROM problem_patterns.dataset_problem_pattern";

  @Autowired
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private DatasetProblemPatternJdbcRepository datasetProblemPatternJdbcRepository;

  @Test
  void upsertUpdateCounterTest() {
    insertValues();

    //Verify empty
    List<DatasetProblemPattern> problemPatterns = jdbcTemplate.query(SELECT_DATASET_PROBLEM_PATTERN_QUERY,
        new DatasetProblemPatternRowMapper());
    assertEquals(0, problemPatterns.size());

    //Upsert with 0
    datasetProblemPatternJdbcRepository.upsertCounter(1, "P1", 0);
    problemPatterns = jdbcTemplate.query(SELECT_DATASET_PROBLEM_PATTERN_QUERY,
        new DatasetProblemPatternRowMapper());
    assertEquals(1, problemPatterns.size());
    assertEquals(0, getOccurrences(problemPatterns, "P1"));

    //Upsert with 1, different pattern
    datasetProblemPatternJdbcRepository.upsertCounter(1, "P2", 1);
    problemPatterns = jdbcTemplate.query(SELECT_DATASET_PROBLEM_PATTERN_QUERY,
        new DatasetProblemPatternRowMapper());
    assertEquals(2, problemPatterns.size());
    assertEquals(1, getOccurrences(problemPatterns, "P2"));

    //Upsert with 1, same pattern
    datasetProblemPatternJdbcRepository.upsertCounter(1, "P2", 1);
    problemPatterns = jdbcTemplate.query(SELECT_DATASET_PROBLEM_PATTERN_QUERY,
        new DatasetProblemPatternRowMapper());
    assertEquals(2, problemPatterns.size());
    assertEquals(2, getOccurrences(problemPatterns, "P2"));

    //Upsert with 5, same pattern
    datasetProblemPatternJdbcRepository.upsertCounter(1, "P2", 5);
    problemPatterns = jdbcTemplate.query(SELECT_DATASET_PROBLEM_PATTERN_QUERY,
        new DatasetProblemPatternRowMapper());
    assertEquals(2, problemPatterns.size());
    assertEquals(7, getOccurrences(problemPatterns, "P2"));

    //Cleanup
    deleteFromTables(jdbcTemplate, "problem_patterns.dataset_problem_pattern", "problem_patterns.execution_point");
    assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "problem_patterns.execution_point"));
    assertEquals(0, jdbcTemplate.query(SELECT_DATASET_PROBLEM_PATTERN_QUERY, new DatasetProblemPatternRowMapper()).size());
  }

  @NotNull
  private Integer getOccurrences(List<DatasetProblemPattern> problemPatterns, String patternIdString) {
    return problemPatterns.stream().filter(
                              datasetProblemPattern -> datasetProblemPattern.getDatasetProblemPatternCompositeKey().getPatternId().equals(
                                  patternIdString))
                          .map(DatasetProblemPattern::getRecordOccurrences).findFirst().orElse(-1);
  }

  private void insertValues() {
    jdbcTemplate.update(
        "INSERT INTO problem_patterns.execution_point (dataset_id, execution_step, execution_timestamp) VALUES (1, 'VALIDATION_EXTERNAL', '2022-01-01 10:10:10.100 +02:00');");
  }

  static class DatasetProblemPatternRowMapper implements RowMapper<DatasetProblemPattern> {

    @Override
    public DatasetProblemPattern mapRow(ResultSet rs, int rowNumber) throws SQLException {
      return new DatasetProblemPattern(
          new DatasetProblemPatternCompositeKey(rs.getInt("execution_point_id"), rs.getString("pattern_id")), null,
          rs.getInt("record_occurrences"));
    }
  }
}
