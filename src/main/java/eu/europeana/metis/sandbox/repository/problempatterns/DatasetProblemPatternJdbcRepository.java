package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPattern;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;

/**
 * Jdbc repository for {@link DatasetProblemPattern}
 */
@Repository
public class DatasetProblemPatternJdbcRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatasetProblemPatternJdbcRepository.class);
  public static final int INSERT_COUNTER_INDEX_POSITION = 3;
  public static final int UPDATE_COUNTER_INDEX_POSITION = 4;
  private final JdbcTemplate jdbcTemplate;

  /**
   * Constructor with required parameters.
   *
   * @param jdbcTemplate the jdbc template
   */
  public DatasetProblemPatternJdbcRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Upserts(Insert or Update) the provided increment value to the counter field and gets the result.
   * @param executionPointId the execution point id
   * @param patternId the pattern id
   * @param incrementValue the increment value
   * @return the upserted counter
   */
  public Integer upsertCounter(int executionPointId, String patternId, int incrementValue) {
    final Integer counter = jdbcTemplate.execute(
        getUpsertCounterPreparedStatementCreator(executionPointId, patternId, incrementValue), preparedStatement ->
        {
          //There should be exactly one row with one value returned
          try (final ResultSet resultSet = preparedStatement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
          }
        });
    LOGGER.debug("Counter after executing update: {}", counter);
    return counter;
  }

  @NotNull
  private PreparedStatementCreator getUpsertCounterPreparedStatementCreator(int executionPointId, String patternId,
      int incrementValue) {
    return connection -> {
      PreparedStatement deleteRedundantStatement = connection.prepareStatement(
          "INSERT INTO problem_patterns.dataset_problem_pattern (execution_point_id, pattern_id, record_occurrences) "
              + "VALUES (?, ?, ?) "
              + "ON CONFLICT (execution_point_id, pattern_id) "
              + "    DO UPDATE SET record_occurrences = problem_patterns.dataset_problem_pattern.record_occurrences + ? "
              + "RETURNING record_occurrences");
      deleteRedundantStatement.setInt(1, executionPointId);
      deleteRedundantStatement.setString(2, patternId);
      deleteRedundantStatement.setInt(INSERT_COUNTER_INDEX_POSITION, incrementValue);
      deleteRedundantStatement.setInt(UPDATE_COUNTER_INDEX_POSITION, incrementValue);
      return deleteRedundantStatement;
    };
  }
}
