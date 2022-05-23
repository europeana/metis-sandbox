package eu.europeana.metis.sandbox.repository.problempatterns;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPattern;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPatternCompositeKey;
import eu.europeana.metis.sandbox.test.utils.PostgresContainerInitializerIT;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
//Only load what we are going to use. This is simpler than specifying all the autoconfigured classes
@SpringBootTest(properties = "spring.main.lazy-initialization=true")
class DatasetProblemPatternJdbcRepositoryIT extends PostgresContainerInitializerIT {

  private static final String SQL_EXECUTION_POINT_COUNT = "SELECT count(*) from problem_patterns.execution_point";
  public static final String SQL_SELECT_DATASET_PROBLEM_PATTERN = "SELECT * FROM problem_patterns.dataset_problem_pattern";

  @Autowired
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private DatasetProblemPatternJdbcRepository datasetProblemPatternJdbcRepository;

  @Test
  void upsertUpdateCounterTest() {
    insertValues();

    //Verify empty
    List<DatasetProblemPattern> problemPatterns = jdbcTemplate.query(SQL_SELECT_DATASET_PROBLEM_PATTERN,
        new DatasetProblemPatternRowMapper());
    assertEquals(0, problemPatterns.size());

    //Upsert with 0
    datasetProblemPatternJdbcRepository.upsertUpdateCounter(1, "P1", 0);
    problemPatterns = jdbcTemplate.query(SQL_SELECT_DATASET_PROBLEM_PATTERN,
        new DatasetProblemPatternRowMapper());
    assertEquals(1, problemPatterns.size());
    assertEquals(0, getOccurrences(problemPatterns, "P1"));

    //Upsert with 1, different pattern
    datasetProblemPatternJdbcRepository.upsertUpdateCounter(1, "P2", 1);
    problemPatterns = jdbcTemplate.query(SQL_SELECT_DATASET_PROBLEM_PATTERN,
        new DatasetProblemPatternRowMapper());
    assertEquals(2, problemPatterns.size());
    assertEquals(1, getOccurrences(problemPatterns, "P2"));

    //Upsert with 1, same pattern
    datasetProblemPatternJdbcRepository.upsertUpdateCounter(1, "P2", 1);
    problemPatterns = jdbcTemplate.query(SQL_SELECT_DATASET_PROBLEM_PATTERN,
        new DatasetProblemPatternRowMapper());
    assertEquals(2, problemPatterns.size());
    assertEquals(2, getOccurrences(problemPatterns, "P2"));

    //Upsert with 5, same pattern
    datasetProblemPatternJdbcRepository.upsertUpdateCounter(1, "P2", 5);
    problemPatterns = jdbcTemplate.query(SQL_SELECT_DATASET_PROBLEM_PATTERN,
        new DatasetProblemPatternRowMapper());
    assertEquals(2, problemPatterns.size());
    assertEquals(7, getOccurrences(problemPatterns, "P2"));

    cleanUp();
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

  public void cleanUp() {
    jdbcTemplate.execute("DELETE FROM problem_patterns.dataset_problem_pattern");
    jdbcTemplate.execute("DELETE FROM problem_patterns.execution_point");
    assertEquals(0, jdbcTemplate.queryForObject(SQL_EXECUTION_POINT_COUNT, Integer.class));
    assertEquals(0, jdbcTemplate.query(SQL_SELECT_DATASET_PROBLEM_PATTERN, new DatasetProblemPatternRowMapper()).size());
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